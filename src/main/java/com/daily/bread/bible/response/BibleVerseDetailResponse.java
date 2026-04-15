package com.daily.bread.bible.response;

public record BibleVerseDetailResponse(String versionId, int bookNumber, String abbrev, String bookName, int chapter,
		int verse, String text) {
}
