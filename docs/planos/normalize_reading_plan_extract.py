#!/usr/bin/env python3
"""
Normaliza texto extraído de PDF (ou outro) para linhas aceites pelo ReadingPlanPdfParser.

Uso típico:
  python normalize_reading_plan_extract.py entrada.txt -o saida-importavel.txt
  python normalize_reading_plan_extract.py entrada.txt -o saida.txt --preset d12-2026 --expect-days 365
  python normalize_reading_plan_extract.py entrada.txt -o saida.txt --overrides presets/meu-plano.json --expect-days 365

Regras de formato: docs/planos/PROMPT_GERAR_PLANO_LEITURA.md

Overrides (JSON): objeto opcional com chaves string do dia (ex. "42") e valor = linha completa já normalizada.
Serve para corrigir casos que o PDF quebra e não batem com o regex genérico.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ENTRY = re.compile(r"^(\d+)\s+(.+?)\s+(\d+)\s*-\s*(\d+)\s*$")
DAY_PREFIX_REST = re.compile(r"^(\d+)\s+(.+)$")
BOOK_RANGE = re.compile(r"^(.+?)\s+(\d+)\s*-\s*(\d+)\s*$")
BOOK_SINGLE_CH = re.compile(r"^(.+?)\s+(\d+)\s*$")
PLUS_SPLIT = re.compile(r"\s*\+\s*")
DEFAULT_HEADER = re.compile(
    r"^(Plano|Dia|Leitura|Lido|PLANO|A Bíblia|\s*$)",
    re.I,
)
BAD_BOOK = frozenset({"dia", "leitura", "lido"})


def load_overrides(path: Path | None) -> dict[int, str]:
    if path is None:
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"Overrides must be a JSON object, got {type(data)}")
    out: dict[int, str] = {}
    for k, v in data.items():
        if not isinstance(k, str) or not isinstance(v, str):
            raise ValueError("Override keys must be string day numbers, values must be strings")
        out[int(k)] = v.strip()
    return out


def fix_day_book_gap(line: str) -> str:
    """Ex.: 112I Crônicas -> 112 I Crônicas"""
    m = re.match(r"^(\d{1,3})([^\d\s.])", line)
    if m:
        return f"{m.group(1)} {m.group(2)}{line[m.end() :]}"
    return line


def fix_book_chapter_gap(line: str) -> str:
    """Ex.: Números10-12 -> Números 10-12"""
    m = re.match(r"^(\d+\s+)(.+?)(\d+)\s*-\s*(\d+)\s*$", line)
    if not m:
        return line
    prefix, book, a, b = m.group(1), m.group(2).strip(), m.group(3), m.group(4)
    if book and book[-1].isdigit():
        return line
    book_rest = line[len(prefix) :]
    m2 = re.match(r"^(.+?)(\d+)\s*-\s*(\d+)\s*$", book_rest)
    if not m2:
        return line
    bk, ca, cb = m2.group(1), m2.group(2), m2.group(3)
    if bk and not bk[-1].isdigit() and re.search(r"[a-zà-ú.]$", bk, re.I):
        return f"{prefix}{bk} {ca}-{cb}"
    return line


def _chapters_ok(a: int, b: int) -> bool:
    return a >= 1 and b >= a


def _book_ok(name: str) -> bool:
    n = name.strip().lower()
    return bool(n) and n not in BAD_BOOK


def _rest_book_optional_range_ok(rest: str) -> bool:
    rest = rest.strip()
    m = BOOK_RANGE.match(rest)
    if m:
        return _book_ok(m.group(1)) and _chapters_ok(int(m.group(2)), int(m.group(3)))
    m = BOOK_SINGLE_CH.match(rest)
    if m:
        c = int(m.group(2))
        return _book_ok(m.group(1)) and c >= 1
    return _book_ok(rest)


def valid_semicolon_day_line(t: str) -> bool:
    if ";" not in t:
        return False
    parts = [p.strip() for p in t.split(";")]
    if len(parts) < 2 or any(not p for p in parts):
        return False
    dm = DAY_PREFIX_REST.match(parts[0])
    if not dm:
        return False
    day = int(dm.group(1))
    if not 1 <= day <= 366:
        return False
    if not _rest_book_optional_range_ok(dm.group(2)):
        return False
    for p in parts[1:]:
        if not _rest_book_optional_range_ok(p):
            return False
    return True


def valid_plus_day_line(t: str) -> bool:
    if "+" not in t:
        return False
    parts = [p.strip() for p in PLUS_SPLIT.split(t)]
    if len(parts) < 2 or any(not p for p in parts):
        return False
    dm = DAY_PREFIX_REST.match(parts[0])
    if not dm:
        return False
    day = int(dm.group(1))
    if not 1 <= day <= 366:
        return False
    if not _rest_book_optional_range_ok(dm.group(2)):
        return False
    for p in parts[1:]:
        if not _rest_book_optional_range_ok(p):
            return False
    return True


def is_valid_import_line(t: str) -> bool:
    return bool(ENTRY.match(t) or valid_semicolon_day_line(t) or valid_plus_day_line(t))


def normalize_line(line: str, overrides: dict[int, str]) -> str | None:
    t = line.strip()
    if not t or DEFAULT_HEADER.match(t):
        return None
    if not re.match(r"^\d+", t):
        return None
    t = fix_day_book_gap(t)
    t = fix_book_chapter_gap(t)
    m = re.match(r"^(\d+)", t)
    if not m:
        return None
    day = int(m.group(1))
    if day in overrides:
        t = overrides[day]
    if ENTRY.match(t):
        return t
    if valid_semicolon_day_line(t):
        return t
    if valid_plus_day_line(t):
        return t
    return None


def run(
    src: Path,
    out: Path,
    *,
    overrides: dict[int, str] | None = None,
    expect_days: int | None = None,
) -> tuple[int, list[int]]:
    """
    Returns (lines_written, missing_days) where missing_days is empty unless expect_days is set.
    """
    ov = overrides or {}
    text = src.read_text(encoding="utf-8")
    seen: dict[int, str] = {}
    for line in text.splitlines():
        n = normalize_line(line, ov)
        if not n or not is_valid_import_line(n):
            continue
        day = int(n.split(maxsplit=1)[0])
        if day in seen:
            continue
        seen[day] = n
    lines_out = [seen[d] for d in sorted(seen)]
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines_out) + "\n", encoding="utf-8")
    missing: list[int] = []
    if expect_days is not None:
        missing = [d for d in range(1, expect_days + 1) if d not in seen]
    return len(lines_out), missing


def main(argv: list[str] | None = None) -> int:
    script_dir = Path(__file__).resolve().parent
    p = argparse.ArgumentParser(
        description="Normaliza extração de texto de plano de leitura para o formato da API Daily Bread.",
    )
    p.add_argument(
        "input",
        type=Path,
        help="Arquivo .txt com o texto extraído (ex.: saída de pdftotext ou pypdf).",
    )
    p.add_argument(
        "-o",
        "--output",
        type=Path,
        default=None,
        help="Arquivo de saída (padrão: <entrada-stem>-importavel.txt no mesmo diretório).",
    )
    p.add_argument(
        "--preset",
        type=str,
        default=None,
        metavar="NAME",
        help="Carrega presets/NAME-overrides.json ao lado deste script (ex.: d12-2026).",
    )
    p.add_argument(
        "--overrides",
        type=Path,
        default=None,
        help="JSON opcional: mapa dia -> linha completa (correções por PDF).",
    )
    p.add_argument(
        "--expect-days",
        type=int,
        default=None,
        metavar="N",
        help="Se definido, lista dias 1..N que não apareceram na saída.",
    )
    args = p.parse_args(argv)
    src = args.input
    if not src.is_file():
        print(f"Erro: arquivo não encontrado: {src}", file=sys.stderr)
        return 1
    out = args.output
    if out is None:
        out = src.with_name(f"{src.stem}-importavel.txt")
    overrides_path = args.overrides
    if args.preset:
        preset_file = script_dir / "presets" / f"{args.preset}-overrides.json"
        if overrides_path is not None:
            print("Erro: use apenas --preset ou --overrides, não os dois.", file=sys.stderr)
            return 1
        overrides_path = preset_file
        if not overrides_path.is_file():
            print(f"Erro: preset não encontrado: {overrides_path}", file=sys.stderr)
            return 1
    try:
        overrides = load_overrides(overrides_path)
    except (json.JSONDecodeError, ValueError) as e:
        print(f"Erro ao ler overrides: {e}", file=sys.stderr)
        return 1
    n_lines, missing = run(src, out, overrides=overrides, expect_days=args.expect_days)
    print(f"Wrote {n_lines} lines to {out}")
    if missing:
        tail = " ..." if len(missing) > 20 else ""
        print(f"Missing days ({len(missing)}): {missing[:20]}{tail}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
