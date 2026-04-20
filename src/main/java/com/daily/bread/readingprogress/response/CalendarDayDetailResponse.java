package com.daily.bread.readingprogress.response;

import java.time.LocalDate;
import java.util.List;

/**
 * @param readWithDelay ver {@link CalendarDayReadResponse#readWithDelay()}
 */
public record CalendarDayDetailResponse(LocalDate date, boolean read, boolean readWithDelay, Integer scheduledDayNumber,
		LocalDate scheduledDate, boolean dayCompleted, List<TodayReadingBlockResponse> blocks) {
}
