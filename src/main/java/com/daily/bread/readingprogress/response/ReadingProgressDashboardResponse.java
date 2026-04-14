package com.daily.bread.readingprogress.response;

import java.util.List;

public record ReadingProgressDashboardResponse(Long planId, String planFilename, int totalPlanDays, int completedDays,
		int annualProgressPercent, int currentStreakDays, int daysRemainingInYear, List<WeekDayStripItemResponse> weekStrip,
		TodayReadingSectionResponse today) {
}
