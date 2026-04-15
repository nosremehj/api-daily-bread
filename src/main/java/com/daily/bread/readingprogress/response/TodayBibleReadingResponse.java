package com.daily.bread.readingprogress.response;

import java.time.LocalDate;
import java.util.List;

public record TodayBibleReadingResponse(LocalDate referenceDate, Integer scheduledDayNumber, LocalDate scheduledDate,
		String versionId, boolean dayCompleted, List<TodayBibleBlockResponse> blocks) {
}
