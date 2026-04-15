package com.daily.bread.readingprogress.response;

public record TodayReadingBlockResponse(Long planDayId, Integer dayNumber, String bookName, Integer bookNumber,
		String bookAbbrev, Integer startChapter, Integer endChapter, String readingText, boolean completed) {
}
