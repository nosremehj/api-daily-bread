package com.daily.bread.readingprogress.response;

import java.time.LocalDate;
import java.util.List;

/**
 * @param readWithDelayDatesInReferenceWeek domingo–sábado da semana de {@code referenceDate} em que houve leitura
 *            registrada na data civil mas não no dia do plano previsto para essa data.
 */
public record ReadingProgressDashboardResponse(Long planId, String planFilename, int totalPlanDays, int completedDays,
		int annualProgressPercent, int currentStreakDays, int daysRemainingInYear, List<WeekDayStripItemResponse> weekStrip,
		List<LocalDate> readWithDelayDatesInReferenceWeek, TodayReadingSectionResponse today) {
}
