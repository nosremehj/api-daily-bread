package com.daily.bread.bible.services;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.daily.bread.bible.response.BibleBookResponse;

@Component
public class BibleBookResolver {

	private final Map<String, Integer> normalizedToNumber = new HashMap<>();
	private final List<String> abbrevsByBookNumber;

	public BibleBookResolver(BibleService bibleService) {
		List<BibleBookResponse> nvi = bibleService.listBooks("nvi");
		this.abbrevsByBookNumber = nvi.stream().map(BibleBookResponse::abbrev).toList();
		for (BibleBookResponse b : nvi) {
			index(b.name(), b.number());
			index(b.abbrev(), b.number());
			index(b.name().replace(" ", ""), b.number());
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
