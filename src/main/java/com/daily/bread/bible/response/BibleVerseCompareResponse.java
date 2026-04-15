package com.daily.bread.bible.response;

import java.util.List;

public record BibleVerseCompareResponse(int bookNumber, String abbrev, String bookName, int chapter, int verse,
		List<BibleVersionVerseLineResponse> versions) {
}
