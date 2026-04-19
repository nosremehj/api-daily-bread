package com.daily.bread.readingprogress.response;

import java.time.LocalDate;
import java.util.List;

public record ReadingStatisticsResponse(Long planId, String planFilename, LocalDate planStartDate, LocalDate periodFrom,
		LocalDate periodTo, int totalPlanDays, int completedDaysInPlan, int daysReadInPeriod, int daysMissedInPeriod,
		boolean hasMissedDaysInPeriod, int currentStreakDays, int longestStreakDays, int annualProgressPercent,
		List<LocalDate> readDatesInPeriod, Integer nextMilestonePercent, Integer daysUntilNextMilestone) {
}
