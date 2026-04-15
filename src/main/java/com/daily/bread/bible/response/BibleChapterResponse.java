package com.daily.bread.bible.response;

import java.util.List;

public record BibleChapterResponse(
		String versionId,
		int bookNumber,
		String abbrev,
		String bookName,
		int chapter,
		List<BibleVerseResponse> verses) {
}
