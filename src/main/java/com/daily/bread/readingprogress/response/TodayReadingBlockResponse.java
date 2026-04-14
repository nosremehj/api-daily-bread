package com.daily.bread.readingprogress.response;

public record TodayReadingBlockResponse(Long planDayId, Integer dayNumber, String bookName, Integer startChapter,
		Integer endChapter, String readingText, boolean completed) {
}
