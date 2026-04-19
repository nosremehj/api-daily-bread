package com.daily.bread.bible.services;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.daily.bread.bible.response.BibleBookResponse;

@Component
public class BibleBookResolver {

	private static final Pattern DIGIT_BOOK_PREFIX = Pattern.compile("^(\\d+)\\s+(.+)$");

	private final Map<String, Integer> normalizedToNumber = new HashMap<>();
	private final List<String> abbrevsByBookNumber;

	public BibleBookResolver(BibleService bibleService) {
		List<BibleBookResponse> nvi = bibleService.listBooks("nvi");
		this.abbrevsByBookNumber = nvi.stream().map(BibleBookResponse::abbrev).toList();
		for (BibleBookResponse b : nvi) {
			index(b.name(), b.number());
			index(b.abbrev(), b.number());
			index(b.name().replace(" ", ""), b.number());
			indexRomanPrefixedAliases(b);
		}
		addAlias("salmos", 19);
	}

	private void index(String label, int number) {
		if (label == null || label.isBlank()) {
			return;
		}
		String n = normalize(label);
		if (!n.isEmpty()) {
			normalizedToNumber.putIfAbsent(n, number);
		}
	}

	private void addAlias(String key, int number) {
		normalizedToNumber.putIfAbsent(key, number);
	}

	/** Planos usam "I Samuel"; NVI usa "1 Samuel". Normaliza para o mesmo número. */
	private void indexRomanPrefixedAliases(BibleBookResponse b) {
		Matcher m = DIGIT_BOOK_PREFIX.matcher(b.name());
		if (!m.matches()) {
			return;
		}
		int n = Integer.parseInt(m.group(1));
		String rest = m.group(2);
		if (n == 1) {
			putRomanAlias("I " + rest, b.number());
		}
		else if (n == 2) {
			putRomanAlias("II " + rest, b.number());
		}
		else if (n == 3) {
			putRomanAlias("III " + rest, b.number());
		}
	}

	private void putRomanAlias(String label, int number) {
		String key = normalize(label);
		if (!key.isEmpty()) {
			normalizedToNumber.putIfAbsent(key, number);
		}
	}

	public Optional<Integer> resolveBookNumber(String bookLabel) {
		if (bookLabel == null || bookLabel.isBlank()) {
			return Optional.empty();
		}
		String n = normalize(bookLabel);
		Integer direct = normalizedToNumber.get(n);
		if (direct != null) {
			return Optional.of(direct);
		}
		String collapsed = n.replace(" ", "");
		Integer c = normalizedToNumber.get(collapsed);
		return Optional.ofNullable(c);
	}

	public Optional<String> abbrevForBook(int bookNumber) {
		if (bookNumber < 1 || bookNumber > abbrevsByBookNumber.size()) {
			return Optional.empty();
		}
		return Optional.of(abbrevsByBookNumber.get(bookNumber - 1));
	}

	static String normalize(String s) {
		String lower = s.toLowerCase(Locale.ROOT).trim();
		String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
		return decomposed.replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
	}
}
