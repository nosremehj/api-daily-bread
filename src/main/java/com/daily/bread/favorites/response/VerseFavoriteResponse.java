package com.daily.bread.favorites.response;

import java.time.Instant;
import java.time.LocalDate;

public record VerseFavoriteResponse(Long id, String versionId, int bookNumber, String bookAbbrev, String bookName,
		int chapterNumber, int verseNumber, String text, LocalDate readingDate, Instant createdAt) {
}
