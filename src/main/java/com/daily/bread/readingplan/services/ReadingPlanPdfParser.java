package com.daily.bread.readingplan.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.daily.bread.readingplan.exceptions.ReadingPlanParseException;

@Component
public class ReadingPlanPdfParser {

	/**
	 * Início de trecho "DIA …": dia + espaço + letra maiúscula (início típico do nome do livro). Evita {@code 1 ano) 1-1}
	 * em que "a" minúscula era lida como nome de livro.
	 */
	private static final Pattern DAY_CHUNK_HEAD_SPACED = Pattern.compile("(?:^|\\s)(\\d{1,3})(?=\\s+\\p{Lu})");
	/**
	 * Dia colado ao livro só com três algarismos (100–366), ex.: {@code 329I Coríntios}.
	 * Dígitos 1–2 + letra ({@code 1p}, {@code 2a} em rodapés) não abrem novo trecho — evitava linhas lixo no dia 1.
	 */
	private static final Pattern DAY_CHUNK_HEAD_GLUED = Pattern
			.compile("(?<![0-9])([12][0-9]{2}|3[0-5][0-9]|36[0-6])(?=\\p{L})");
	/** Aceita {@code 329I Coríntios} após o número do dia (espaço opcional). */
	private static final Pattern DAY_PREFIX = Pattern.compile("^(\\d+)\\s*(.+)$");
	private static final Pattern BOOK_RANGE = Pattern.compile("^(.+?)\\s+(\\d+)\\s*-\\s*(\\d+)\\s*$");
	/** Primeiro intervalo quando o PDF cola texto após {@code Livro cap-cap} na mesma linha. */
	private static final Pattern FIRST_BOOK_RANGE_PREFIX = Pattern.compile("^(.+?)\\s+(\\d+)\\s*-\\s*(\\d+)\\b");
	private static final Pattern BOOK_SINGLE_CH = Pattern.compile("^(.+?)\\s+(\\d+)\\s*$");
	private static final Pattern PLUS_SPLIT = Pattern.compile("\\s*\\+\\s*");
	private static final int MIN_DAY = 1;
	private static final int MAX_DAY = 366;

	private record Partial(int dayNumber, String bookName, Integer startChapter, Integer endChapter,
			String readingText) {
	}

	public List<ParsedPlanRow> parse(byte[] pdfBytes) {
		return parsePlainText(extractText(pdfBytes));
	}

	/**
	 * Mesmo algoritmo usado após extração do PDF (útil em testes e para texto exportado).
	 * <p>
	 * Vários trechos no mesmo dia: use {@code ;} ou {@code +} entre trechos; o número do dia só no primeiro. Sem
	 * capítulos (só o nome do livro) = livro inteiro na importação.
	 */
	public List<ParsedPlanRow> parsePlainText(String text) {
		List<Partial> partials = new ArrayList<>();
		for (String line : text.split("\\R")) {
			String t = line.trim();
			if (t.isEmpty()) {
				continue;
			}
			if (tryCollectDelimitedLine(t, partials, ';')) {
				continue;
			}
			if (tryCollectDelimitedLine(t, partials, '+')) {
				continue;
			}
			collectFromPossiblyMergedDayLine(t, partials);
		}
		if (partials.isEmpty()) {
			String flat = text.replaceAll("\\s+", " ").trim();
			if (!flat.isEmpty()) {
				if (!tryCollectDelimitedLine(flat, partials, ';') && !tryCollectDelimitedLine(flat, partials, '+')) {
					collectFromPossiblyMergedDayLine(flat, partials);
				}
			}
		}
		if (partials.isEmpty()) {
			throw new ReadingPlanParseException(
					"Não foi possível extrair linhas do plano. Use: DIA LIVRO [cap-cap] com trechos extra separados por ; ou + (capítulos omitidos = livro inteiro).");
		}
		List<ParsedPlanRow> out = assignSegmentIndices(partials);
		out.sort(Comparator.comparingInt(ParsedPlanRow::dayNumber).thenComparingInt(ParsedPlanRow::segmentIndex));
		return out;
	}

	private List<ParsedPlanRow> assignSegmentIndices(List<Partial> partials) {
		Map<Integer, Integer> nextSeg = new HashMap<>();
		List<ParsedPlanRow> out = new ArrayList<>(partials.size());
		for (Partial p : partials) {
			int seg = nextSeg.getOrDefault(p.dayNumber(), 0);
			nextSeg.put(p.dayNumber(), seg + 1);
			out.add(new ParsedPlanRow(p.dayNumber(), seg, p.bookName(), p.startChapter(), p.endChapter(),
					p.readingText()));
		}
		return out;
	}

	/** Trechos separados por {@code ;} ou {@code +}. */
	private boolean tryCollectDelimitedLine(String line, List<Partial> out, char delimiter) {
		if (delimiter == ';' && !line.contains(";")) {
			return false;
		}
		if (delimiter == '+' && !line.contains("+")) {
			return false;
		}
		String[] pieces = delimiter == ';' ? line.split(";", -1) : PLUS_SPLIT.split(line, -1);
		if (pieces.length < 2) {
			return false;
		}
		Integer dayHolder = null;
		for (int i = 0; i < pieces.length; i++) {
			String raw = pieces[i].trim();
			if (raw.isEmpty()) {
				return false;
			}
			if (i == 0) {
				Matcher dm = DAY_PREFIX.matcher(raw);
				if (!dm.matches()) {
					return false;
				}
				int day = Integer.parseInt(dm.group(1));
				if (day < MIN_DAY || day > MAX_DAY) {
					return false;
				}
				dayHolder = day;
				int before = out.size();
				collectPartialsForReading(day, dm.group(2).trim(), out);
				if (out.size() == before) {
					return false;
				}
			}
			else {
				if (dayHolder == null) {
					return false;
				}
				int before = out.size();
				collectPartialsForReading(dayHolder, raw, out);
				if (out.size() == before) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Uma leitura por trecho "DIA …". Evita que {@code 274 Obadias e Jonas 305 Lucas 16-18} vire um único match com
	 * capítulos do dia 305 no dia 274.
	 */
	private void collectFromPossiblyMergedDayLine(String line, List<Partial> out) {
		for (String chunk : splitConcatenatedDayChunks(line)) {
			parseOneDayChunk(chunk, out);
		}
	}

	private List<String> splitConcatenatedDayChunks(String line) {
		TreeSet<Integer> starts = new TreeSet<>();
		addValidDayStarts(line, DAY_CHUNK_HEAD_SPACED, starts);
		addValidDayStarts(line, DAY_CHUNK_HEAD_GLUED, starts);
		if (starts.isEmpty()) {
			return List.of(line);
		}
		List<String> chunks = new ArrayList<>(starts.size());
		List<Integer> startList = new ArrayList<>(starts);
		for (int i = 0; i < startList.size(); i++) {
			int a = startList.get(i);
			int b = i + 1 < startList.size() ? startList.get(i + 1) : line.length();
			chunks.add(line.substring(a, b).trim());
		}
		return chunks;
	}

	private static void addValidDayStarts(String line, Pattern head, TreeSet<Integer> starts) {
		Matcher m = head.matcher(line);
		while (m.find()) {
			int day = Integer.parseInt(m.group(1));
			if (day >= MIN_DAY && day <= MAX_DAY) {
				starts.add(m.start(1));
			}
		}
	}

	private void parseOneDayChunk(String segment, List<Partial> out) {
		segment = segment.trim();
		if (segment.isEmpty()) {
			return;
		}
		Matcher dm = DAY_PREFIX.matcher(segment);
		if (!dm.matches()) {
			return;
		}
		int day = Integer.parseInt(dm.group(1));
		if (day < MIN_DAY || day > MAX_DAY) {
			return;
		}
		String rest = dm.group(2).trim();
		if (rest.isEmpty()) {
			return;
		}
		collectPartialsForReading(day, rest, out);
	}

	private void collectPartialsForReading(int day, String rest, List<Partial> out) {
		Matcher range = BOOK_RANGE.matcher(rest);
		if (range.matches()) {
			String book = range.group(1).trim();
			int a = Integer.parseInt(range.group(2));
			int b = Integer.parseInt(range.group(3));
			if (book.isEmpty() || isHeaderToken(book) || a < 1 || b < a || !startsWithUppercaseBook(book)) {
				return;
			}
			out.add(new Partial(day, book, a, b, book + " " + a + "-" + b));
			return;
		}
		Matcher prefixRange = FIRST_BOOK_RANGE_PREFIX.matcher(rest);
		if (prefixRange.find() && prefixRange.start() == 0) {
			String book = prefixRange.group(1).trim();
			int a = Integer.parseInt(prefixRange.group(2));
			int b = Integer.parseInt(prefixRange.group(3));
			if (!book.isEmpty() && !isHeaderToken(book) && a >= 1 && b >= a && startsWithUppercaseBook(book)) {
				out.add(new Partial(day, book, a, b, book + " " + a + "-" + b));
				String tail = rest.substring(prefixRange.end()).trim();
				if (!tail.isEmpty() && startsWithUppercaseBook(tail)) {
					collectPartialsForReading(day, tail, out);
				}
				return;
			}
		}
		Matcher single = BOOK_SINGLE_CH.matcher(rest);
		if (single.matches()) {
			String book = single.group(1).trim();
			int c = Integer.parseInt(single.group(2));
			if (book.isEmpty() || isHeaderToken(book) || c < 1 || !startsWithUppercaseBook(book)) {
				return;
			}
			out.add(new Partial(day, book, c, c, book + " " + c + "-" + c));
			return;
		}
		collectWholeBookLabels(day, rest, out);
	}

	/** Ex.: {@code Obadias e Jonas} ou {@code Sofonias e Ageu} no mesmo dia, capítulos omitidos = livros inteiros. */
	private void collectWholeBookLabels(int day, String bookField, List<Partial> out) {
		if (bookField.contains(" e ")) {
			for (String piece : bookField.split("\\s+e\\s+")) {
				String p = piece.trim();
				if (!p.isEmpty()) {
					collectWholeBookLabels(day, p, out);
				}
			}
			return;
		}
		String book = bookField.trim();
		if (book.isEmpty() || isHeaderToken(book) || !startsWithUppercaseBook(book)) {
			return;
		}
		out.add(new Partial(day, book, null, null, book));
	}

	private boolean isHeaderToken(String book) {
		String lower = book.toLowerCase();
		return lower.equals("dia") || lower.equals("leitura") || lower.equals("lido");
	}

	private static boolean startsWithUppercaseBook(String s) {
		if (s.isEmpty()) {
			return false;
		}
		int cp = s.codePointAt(0);
		return Character.isUpperCase(cp);
	}

	private String extractText(byte[] pdfBytes) {
		try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);
			return stripper.getText(doc);
		}
		catch (IOException e) {
			throw new ReadingPlanParseException("Falha ao ler o PDF: " + e.getMessage(), e);
		}
	}
}
