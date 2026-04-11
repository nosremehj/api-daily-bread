package com.daily.bread.readingplan.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.daily.bread.readingplan.exceptions.ReadingPlanParseException;

@Component
public class ReadingPlanPdfParser {

	private static final Pattern READING_ENTRY = Pattern.compile("(\\d+)\\s+(.+?)\\s+(\\d+)\\s*-\\s*(\\d+)");
	private static final int MIN_DAY = 1;
	private static final int MAX_DAY = 366;

	public List<ParsedReadingDay> parse(byte[] pdfBytes) {
		return parsePlainText(extractText(pdfBytes));
	}

	/**
	 * Expõe o mesmo algoritmo usado após extração do PDF (útil em testes e para PDFs exportados como texto).
	 */
	public List<ParsedReadingDay> parsePlainText(String text) {
		Map<Integer, ParsedReadingDay> byDay = new LinkedHashMap<>();
		for (String line : text.split("\\R")) {
			String t = line.trim();
			if (!t.isEmpty()) {
				collectMatches(t, byDay);
			}
		}
		if (byDay.isEmpty()) {
			String flat = text.replaceAll("\\s+", " ").trim();
			if (!flat.isEmpty()) {
				collectMatches(flat, byDay);
			}
		}
		if (byDay.isEmpty()) {
			throw new ReadingPlanParseException(
					"Não foi possível extrair linhas do plano. O PDF precisa ter texto selecionável no formato: número do dia, livro e faixa de capítulos (ex.: 1 Gênesis 1-3).");
		}
		List<ParsedReadingDay> out = new ArrayList<>(byDay.values());
		out.sort(Comparator.comparingInt(ParsedReadingDay::dayNumber));
		return out;
	}

	private void collectMatches(String chunk, Map<Integer, ParsedReadingDay> byDay) {
		Matcher matcher = READING_ENTRY.matcher(chunk);
		while (matcher.find()) {
			int day = Integer.parseInt(matcher.group(1));
			if (day < MIN_DAY || day > MAX_DAY) {
				continue;
			}
			String book = matcher.group(2).trim();
			if (book.isEmpty() || isHeaderToken(book)) {
				continue;
			}
			int start = Integer.parseInt(matcher.group(3));
			int end = Integer.parseInt(matcher.group(4));
			if (start < 1 || end < start) {
				continue;
			}
			String readingText = book + " " + start + "-" + end;
			ParsedReadingDay parsed = new ParsedReadingDay(day, book, start, end, readingText);
			ParsedReadingDay existing = byDay.putIfAbsent(day, parsed);
			if (existing != null && !existing.equals(parsed)) {
				throw new ReadingPlanParseException("Dia duplicado no PDF com leituras diferentes: " + day);
			}
		}
	}

	private boolean isHeaderToken(String book) {
		String lower = book.toLowerCase();
		return lower.equals("dia") || lower.equals("leitura") || lower.equals("lido");
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
