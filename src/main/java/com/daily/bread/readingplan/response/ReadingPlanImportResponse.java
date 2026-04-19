package com.daily.bread.readingplan.response;

import java.time.Instant;

public record ReadingPlanImportResponse(Long id, String originalFilename, Instant importedAt, int calendarDaysImported,
		int segmentsImported) {
}
