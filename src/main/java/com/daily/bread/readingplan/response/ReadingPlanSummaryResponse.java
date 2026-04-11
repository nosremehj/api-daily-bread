package com.daily.bread.readingplan.response;

import java.time.Instant;

public record ReadingPlanSummaryResponse(Long id, String originalFilename, Instant importedAt, long dayCount) {
}
