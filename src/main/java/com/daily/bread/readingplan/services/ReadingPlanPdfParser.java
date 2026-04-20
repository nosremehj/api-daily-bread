package com.daily.bread.readingplan.services;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

	private static final int PREVIEW_RELEVANT_LINE_CAP = 40;

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
		return parseBundled(pdfBytes).rows();
	}

	/**
	 * Uma única passagem pelo PDF: texto bruto/normalizado + linhas relevantes para depuração (ex.: resposta HTTP em dev).
	 * Usa duas extrações PDFBox (com/sem ordenação por posição) e funde por dia — uma delas costuma recuperar linhas
	 * perdidas (ex.: dia 258 colado a {@code 34-36} como {@code 34-36258}).
	 */
	public ReadingPlanPdfExtractBundle parseBundled(byte[] pdfBytes) {
		try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
			String rawSorted = stripDocument(doc, true);
			String rawUnsorted = stripDocument(doc, false);
			String normSorted = normalizeExtractedPlanText(rawSorted);
			String normUnsorted = normalizeExtractedPlanText(rawUnsorted);
			List<ParsedPlanRow> rowsSorted = safeParseNormalized(normSorted);
			List<ParsedPlanRow> rowsUnsorted = safeParseNormalized(normUnsorted);
			List<ParsedPlanRow> rows = mergePlanRowsByDayPreferFirst(rowsSorted, rowsUnsorted);
			if (rows.isEmpty()) {
				throw new ReadingPlanParseException(
						"Não foi possível extrair linhas do plano (nem com ordenação por posição).");
			}
			List<String> preview = Collections.unmodifiableList(buildRelevantLineSnippets(rawSorted, true));
			return new ReadingPlanPdfExtractBundle(rows, rawSorted, normSorted, preview);
		}
		catch (IOException e) {
			throw new ReadingPlanParseException("Falha ao ler o PDF: " + e.getMessage(), e);
		}
	}

	/**
	 * Mesmo algoritmo usado após extração do PDF (útil em testes e para texto exportado).
	 * <p>
	 * Vários trechos no mesmo dia: use {@code ;} ou {@code +} entre trechos; o número do dia só no primeiro. Sem
	 * capítulos (só o nome do livro) = livro inteiro na importação.
	 */
	public List<ParsedPlanRow> parsePlainText(String text) {
		return parsePlainTextFromNormalized(normalizeExtractedPlanText(text));
	}

	private List<ParsedPlanRow> parsePlainTextFromNormalized(String text) {
		List<Partial> partials = new ArrayList<>();
		Set<Integer> daysWithRows = new HashSet<>();
		int[] lastMaxDay = { 0 };
		/* PDF em colunas: várias linhas só com o índice (358, 359, …) antes do bloco de leituras; um único int
		 * sobrescrevia o anterior e a leitura do 358 ia parar no 359. */
		Deque<Integer> pendingDayNumbers = new ArrayDeque<>();
		for (String line : text.split("\\R")) {
			String t = repairGluedChapterRangeFollowedByDay(line).trim();
			if (t.isEmpty()) {
				continue;
			}
			if (isDayIndexOnlyLine(t)) {
				pendingDayNumbers.addLast(Integer.parseInt(t));
				continue;
			}
			if (DAY_PREFIX.matcher(t).matches()) {
				pendingDayNumbers.clear();
			}
			else if (!pendingDayNumbers.isEmpty()) {
				t = pendingDayNumbers.pollFirst() + " " + t;
			}
			else {
				Integer hole = sandwichedMissingDay(lastMaxDay[0], daysWithRows);
				if (hole != null && looksLikeDeferredReadingLine(t)) {
					t = hole + " " + t;
				}
			}
			int before = partials.size();
			if (tryCollectDelimitedLine(t, partials, ';')) {
				registerNewDayRows(partials, before, daysWithRows, lastMaxDay);
				continue;
			}
			before = partials.size();
			if (tryCollectDelimitedLine(t, partials, '+')) {
				registerNewDayRows(partials, before, daysWithRows, lastMaxDay);
				continue;
			}
			before = partials.size();
			collectFromPossiblyMergedDayLine(t, partials);
			registerNewDayRows(partials, before, daysWithRows, lastMaxDay);
		}
		if (partials.isEmpty()) {
			String flat = repairGluedChapterRangeFollowedByDay(text.replaceAll("\\s+", " ")).trim();
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
			String book = maybeCapitalizeBookName(range.group(1).trim());
			int a = Integer.parseInt(range.group(2));
			int b = Integer.parseInt(range.group(3));
			if (book.isEmpty() || isHeaderToken(book) || a < 1 || b < a || !isPlausibleBookLabelStart(book)) {
				return;
			}
			out.add(new Partial(day, book, a, b, book + " " + a + "-" + b));
			return;
		}
		Matcher prefixRange = FIRST_BOOK_RANGE_PREFIX.matcher(rest);
		if (prefixRange.find() && prefixRange.start() == 0) {
			String book = maybeCapitalizeBookName(prefixRange.group(1).trim());
			int a = Integer.parseInt(prefixRange.group(2));
			int b = Integer.parseInt(prefixRange.group(3));
			if (!book.isEmpty() && !isHeaderToken(book) && a >= 1 && b >= a && isPlausibleBookLabelStart(book)) {
				out.add(new Partial(day, book, a, b, book + " " + a + "-" + b));
				String tail = rest.substring(prefixRange.end()).trim();
				if (!tail.isEmpty() && isPlausibleBookLabelStart(maybeCapitalizeBookName(tail))) {
					collectPartialsForReading(day, tail, out);
				}
				return;
			}
		}
		Matcher single = BOOK_SINGLE_CH.matcher(rest);
		if (single.matches()) {
			String book = maybeCapitalizeBookName(single.group(1).trim());
			int c = Integer.parseInt(single.group(2));
			if (book.isEmpty() || isHeaderToken(book) || c < 1 || !isPlausibleBookLabelStart(book)) {
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
			String[] rawPieces = bookField.split("\\s+e\\s+");
			for (int i = 0; i < rawPieces.length; i++) {
				String p = rawPieces[i].trim();
				if (p.isEmpty()) {
					continue;
				}
				/* Plano D12: "II e III João" = II João + III João (sufixo só no segundo trecho). */
				if (p.matches("^I{1,3}$") && i + 1 < rawPieces.length) {
					String next = rawPieces[i + 1].trim();
					int sp = next.indexOf(' ');
					if (sp > 0) {
						String suffix = next.substring(sp + 1).trim();
						if (!suffix.isEmpty()) {
							p = p + " " + suffix;
						}
					}
				}
				collectWholeBookLabels(day, p, out);
			}
			return;
		}
		String book = maybeCapitalizeBookName(bookField.trim());
		if (book.isEmpty() || isHeaderToken(book) || !isPlausibleBookLabelStart(book)) {
			return;
		}
		out.add(new Partial(day, book, null, null, book));
	}

	private boolean isHeaderToken(String book) {
		String lower = book.toLowerCase(Locale.ROOT);
		return lower.equals("dia") || lower.equals("leitura") || lower.equals("lido")
				|| lower.startsWith("ano)");
	}

	/** PDFs às vezes trazem "ezequiel" minúsculo; cabeçalhos tipo "dia" continuam bloqueados por {@link #isHeaderToken}. */
	private static String maybeCapitalizeBookName(String book) {
		if (book.isEmpty()) {
			return book;
		}
		int cp = book.codePointAt(0);
		if (Character.isUpperCase(cp) || Character.isTitleCase(cp)) {
			return book;
		}
		if (!Character.isLetter(cp)) {
			return book;
		}
		int len = Character.charCount(cp);
		return book.substring(0, len).toUpperCase(Locale.ROOT) + book.substring(len);
	}

	private static boolean isPlausibleBookLabelStart(String s) {
		if (s.isEmpty()) {
			return false;
		}
		int cp = s.codePointAt(0);
		return Character.isLetter(cp);
	}

	/**
	 * Corrige hífen entre capítulos colado ao número do dia seguinte: {@code 34-36258 Ezequiel} →
	 * {@code 34-36 258 Ezequiel} (comum quando a célula do PDF junta fim de intervalo com o índice do dia).
	 */
	private static String repairGluedChapterRangeFollowedByDay(String line) {
		if (line == null || !line.contains("-")) {
			return line;
		}
		Pattern p = Pattern.compile("(\\d{1,3})-(\\d{2})(\\d{3})(?=\\s+\\p{L})");
		Matcher m = p.matcher(line);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			int startCh = Integer.parseInt(m.group(1));
			int endCh = Integer.parseInt(m.group(2));
			int day = Integer.parseInt(m.group(3));
			if (day >= MIN_DAY && day <= MAX_DAY && endCh >= startCh && endCh <= 200) {
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "-" + m.group(2) + " " + m.group(3)));
			}
			else {
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static List<ParsedPlanRow> mergePlanRowsByDayPreferFirst(List<ParsedPlanRow> primary,
			List<ParsedPlanRow> secondary) {
		if (secondary == null || secondary.isEmpty()) {
			return primary;
		}
		if (primary == null || primary.isEmpty()) {
			return reassignSegmentIndices(new ArrayList<>(secondary));
		}
		Map<Integer, List<ParsedPlanRow>> a = new HashMap<>();
		for (ParsedPlanRow r : primary) {
			a.computeIfAbsent(r.dayNumber(), k -> new ArrayList<>()).add(r);
		}
		Map<Integer, List<ParsedPlanRow>> b = new HashMap<>();
		for (ParsedPlanRow r : secondary) {
			b.computeIfAbsent(r.dayNumber(), k -> new ArrayList<>()).add(r);
		}
		TreeSet<Integer> days = new TreeSet<>();
		days.addAll(a.keySet());
		days.addAll(b.keySet());
		List<ParsedPlanRow> merged = new ArrayList<>();
		for (int d : days) {
			List<ParsedPlanRow> chunk = a.getOrDefault(d, List.of());
			if (chunk.isEmpty()) {
				chunk = b.getOrDefault(d, List.of());
			}
			merged.addAll(chunk);
		}
		return reassignSegmentIndices(merged);
	}

	private static List<ParsedPlanRow> reassignSegmentIndices(List<ParsedPlanRow> rows) {
		Map<Integer, Integer> nextSeg = new HashMap<>();
		List<ParsedPlanRow> out = new ArrayList<>(rows.size());
		rows.sort(Comparator.comparingInt(ParsedPlanRow::dayNumber).thenComparingInt(ParsedPlanRow::segmentIndex));
		for (ParsedPlanRow r : rows) {
			int seg = nextSeg.getOrDefault(r.dayNumber(), 0);
			nextSeg.put(r.dayNumber(), seg + 1);
			out.add(new ParsedPlanRow(r.dayNumber(), seg, r.bookName(), r.startChapter(), r.endChapter(),
					r.readingText()));
		}
		return out;
	}

	private List<ParsedPlanRow> safeParseNormalized(String normalized) {
		try {
			return parsePlainTextFromNormalized(normalized);
		}
		catch (ReadingPlanParseException e) {
			return List.of();
		}
	}

	private static String stripDocument(PDDocument doc, boolean sortByPosition) throws IOException {
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.setSortByPosition(sortByPosition);
		return stripper.getText(doc);
	}

	/**
	 * PDFs em tabela às vezes colocam só o número do dia numa linha e a leitura na seguinte; {@link #DAY_PREFIX} exige
	 * texto após o dia, então {@code 358} sozinho seria ignorado.
	 */
	private static boolean isDayIndexOnlyLine(String trimmedLine) {
		if (!trimmedLine.matches("^\\d{1,3}$")) {
			return false;
		}
		int d = Integer.parseInt(trimmedLine);
		return d >= MIN_DAY && d <= MAX_DAY;
	}

	/**
	 * Padrão típico do PDF D12: já temos os dias {@code lastMax-2} e {@code lastMax}, mas falta {@code lastMax-1}
	 * (ex.: 357 e 359 extraídos antes de {@code II e III João + Judas}).
	 */
	private static Integer sandwichedMissingDay(int lastMax, Set<Integer> seen) {
		if (lastMax < MIN_DAY + 2) {
			return null;
		}
		int hole = lastMax - 1;
		int belowHole = lastMax - 2;
		if (!seen.contains(lastMax) || seen.contains(hole) || !seen.contains(belowHole)) {
			return null;
		}
		return hole;
	}

	/** Linha só com leitura (sem número do dia), típica de epístolas curtas agrupadas. */
	private static boolean looksLikeDeferredReadingLine(String t) {
		if (t.length() < 6 || DAY_PREFIX.matcher(t).matches() || isDayIndexOnlyLine(t)) {
			return false;
		}
		String lower = t.toLowerCase(Locale.ROOT);
		if (lower.startsWith("dia ") || lower.contains("plano bíblico") || lower.contains("capa a capa")) {
			return false;
		}
		return t.contains(" e ") && (t.contains("+") || t.contains(";"));
	}

	private static void registerNewDayRows(List<Partial> partials, int before, Set<Integer> daysWithRows, int[] lastMaxDay) {
		for (int i = before; i < partials.size(); i++) {
			int d = partials.get(i).dayNumber();
			daysWithRows.add(d);
			if (d > lastMaxDay[0]) {
				lastMaxDay[0] = d;
			}
		}
	}

	/** @param withLineIndex {@code true} → {@code "12: texto da linha"} */
	private static List<String> buildRelevantLineSnippets(String blob, boolean withLineIndex) {
		if (blob == null || blob.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		String[] lines = blob.split("\\R", -1);
		for (int idx = 0; idx < lines.length; idx++) {
			if (!isPdfImportRelevantLine(lines[idx]) || out.size() >= PREVIEW_RELEVANT_LINE_CAP) {
				continue;
			}
			String trimmed = lines[idx].trim();
			out.add(withLineIndex ? idx + ": " + trimmed : trimmed);
		}
		return out;
	}

	private static boolean isPdfImportRelevantLine(String line) {
		if (line == null || line.isBlank()) {
			return false;
		}
		String t = line.trim();
		String lower = t.toLowerCase(Locale.ROOT);
		if (lower.contains("joão") || lower.contains("joao") || lower.contains("judas")) {
			return true;
		}
		if (lower.contains("apocalipse") && (t.contains("358") || t.contains("359") || t.contains("360"))) {
			return true;
		}
		if (t.contains("358") || t.contains("359") || t.contains("360")) {
			return true;
		}
		if (t.matches("(?i).*II\\s+e\\s+III.*")) {
			return true;
		}
		if (t.contains("Ⅱ") || t.contains("Ⅲ") || t.contains("\u2161") || t.contains("\u2162")) {
			return true;
		}
		if (t.contains("+") && (lower.contains("joão") || lower.contains("joao") || lower.contains("judas"))) {
			return true;
		}
		if (t.contains("\uFF0B") || t.contains("\uFE62")) {
			return true;
		}
		return false;
	}

	/**
	 * PDFs usam com frequência {@code Ⅱ}/{@code Ⅲ} (U+2161/U+2162) e “+” largo (U+FF0B). Isso fazia
	 * {@link Character#isUpperCase(int)} falhar e {@code tryCollectDelimitedLine(..., '+')} não reconhecer o separador.
	 */
	static String normalizeExtractedPlanText(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		String n = Normalizer.normalize(text, Normalizer.Form.NFKC);
		StringBuilder sb = new StringBuilder(n.length() + 32);
		for (int i = 0; i < n.length(); ) {
			int cp = n.codePointAt(i);
			String asciiRoman = typographicRomanToAscii(cp);
			if (asciiRoman != null) {
				sb.append(asciiRoman);
			}
			else {
				switch (cp) {
					case '\uFF0B', '\uFE62', '\u207A' -> sb.append('+');
					default -> sb.appendCodePoint(cp);
				}
			}
			i += Character.charCount(cp);
		}
		return sb.toString();
	}

	private static String typographicRomanToAscii(int cp) {
		return switch (cp) {
			case '\u2160', '\u2170' -> "I";
			case '\u2161', '\u2171' -> "II";
			case '\u2162', '\u2172' -> "III";
			case '\u2163', '\u2173' -> "IV";
			case '\u2164', '\u2174' -> "V";
			case '\u2165', '\u2175' -> "VI";
			case '\u2166', '\u2176' -> "VII";
			case '\u2167', '\u2177' -> "VIII";
			case '\u2168', '\u2178' -> "IX";
			case '\u2169', '\u2179' -> "X";
			case '\u216A', '\u217A' -> "XI";
			case '\u216B', '\u217B' -> "XII";
			default -> null;
		};
	}

}
