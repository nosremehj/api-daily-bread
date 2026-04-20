package com.daily.bread.readingplan.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ReadingPlanImportResponse(Long id, String originalFilename, Instant importedAt, int calendarDaysImported,
		int segmentsImported, List<String> pdfExtractPreviewLines) {
}
