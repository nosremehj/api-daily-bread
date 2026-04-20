package com.daily.bread.readingprogress.response;

import java.util.List;

import com.daily.bread.bible.response.BibleChapterResponse;

public record TodayBibleBlockResponse(Long planDayId, int segmentIndex, Integer dayNumber, String bookName,
		Integer bookNumber, String bookAbbrev, Integer startChapter, Integer endChapter, String readingText,
		boolean completed, List<BibleChapterResponse> chapters) {
}
