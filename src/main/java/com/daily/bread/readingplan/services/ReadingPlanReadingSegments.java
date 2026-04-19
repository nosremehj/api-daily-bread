package com.daily.bread.readingplan.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta {@code reading_plan_days.reading_text}: um ou mais trechos {@code LIVRO cap-cap} separados por {@code ;}
 * quando o plano define **várias leituras de livros diferentes no mesmo dia** (em qualquer dia 1…366).
 */
public final class ReadingPlanReadingSegments {

	private static final Pattern ONE_SEGMENT = Pattern.compile("^(.+?)\\s+(\\d+)\\s*-\\s*(\\d+)\\s*$");

	public record Segment(String bookName, int startChapter, int endChapter, String segmentText) {
	}

	private ReadingPlanReadingSegments() {
	}

	/**
	 * @return lista vazia se {@code readingText} for inválido (o chamador deve usar os campos do dia)
	 */
	public static List<Segment> parse(String readingText) {
		if (readingText == null || readingText.isBlank()) {
			return List.of();
		}
		String[] parts = readingText.split(";");
		List<Segment> out = new ArrayList<>(parts.length);
		for (String raw : parts) {
			String p = raw.trim();
			if (p.isEmpty()) {
				return List.of();
			}
			Segment seg = parseOne(p);
			if (seg == null) {
				return List.of();
			}
			out.add(seg);
		}
		return List.copyOf(out);
	}

	private static Segment parseOne(String part) {
		Matcher m = ONE_SEGMENT.matcher(part);
		if (!m.matches()) {
			return null;
		}
		String book = m.group(1).trim();
		int a = Integer.parseInt(m.group(2));
		int b = Integer.parseInt(m.group(3));
		if (book.isEmpty() || a < 1 || b < a) {
			return null;
		}
		return new Segment(book, a, b, book + " " + a + "-" + b);
	}
}
