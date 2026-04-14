package com.daily.bread.readingprogress.response;

import java.time.LocalDate;
import java.util.List;

public record TodayReadingSectionResponse(LocalDate referenceDate, Integer scheduledDayNumber, LocalDate scheduledDate,
		List<TodayReadingBlockResponse> blocks) {
}
