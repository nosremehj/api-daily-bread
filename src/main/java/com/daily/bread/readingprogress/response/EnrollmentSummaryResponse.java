package com.daily.bread.readingprogress.response;

import java.time.Instant;
import java.time.LocalDate;

public record EnrollmentSummaryResponse(Long enrollmentId, Long planId, String planFilename, LocalDate planStartDate,
		Instant enrolledAt) {
}
