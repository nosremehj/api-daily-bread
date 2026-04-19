#!/usr/bin/env python3
"""
Atalho para o plano D12 2026: chama normalize_reading_plan_extract.py com caminhos e preset padrão.

Para outros planos, use diretamente:
  python normalize_reading_plan_extract.py <extraido.txt> -o <importavel.txt> [--overrides ...]
"""
from pathlib import Path

from normalize_reading_plan_extract import load_overrides, run

if __name__ == "__main__":
    here = Path(__file__).resolve().parent
    src = here / "plano-biblico-d12-2026-extracted.txt"
    out = here / "plano-biblico-d12-2026-importavel.txt"
    preset = here / "presets" / "d12-2026-overrides.json"

    overrides = load_overrides(preset if preset.is_file() else None)
    n_lines, missing = run(src, out, overrides=overrides, expect_days=365)
    print(f"Wrote {n_lines} lines to {out}")
    if missing:
        tail = " ..." if len(missing) > 20 else ""
        print(f"Missing days ({len(missing)}): {missing[:20]}{tail}")
