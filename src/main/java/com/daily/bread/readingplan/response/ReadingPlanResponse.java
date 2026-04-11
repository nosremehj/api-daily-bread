package com.daily.bread.readingplan.response;

import java.time.Instant;
import java.util.List;

public record ReadingPlanResponse(Long id, String originalFilename, Instant importedAt,
		List<ReadingPlanDayResponse> days) {
}
