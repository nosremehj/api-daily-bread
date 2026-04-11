package com.daily.bread.readingplan.response;

public record ReadingPlanDayResponse(Long id, Integer dayNumber, String bookName, Integer startChapter,
		Integer endChapter, String readingText, boolean completed) {
}
