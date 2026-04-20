package com.daily.bread.readingprogress.response;

import java.time.LocalDate;
import java.util.List;

/**
 * @param readWithDelayDatesInPeriod datas civis no período com leitura registrada apenas com atraso (fora do dia
 *            agendado para o trecho concluído nessa data).
 */
public record ReadingStatisticsResponse(Long planId, String planFilename, LocalDate planStartDate, LocalDate periodFrom,
		LocalDate periodTo, int totalPlanDays, int completedDaysInPlan, int daysReadInPeriod, int daysMissedInPeriod,
		boolean hasMissedDaysInPeriod, int currentStreakDays, int longestStreakDays, int annualProgressPercent,
		List<LocalDate> readDatesInPeriod, List<LocalDate> readWithDelayDatesInPeriod, Integer nextMilestonePercent,
		Integer daysUntilNextMilestone) {
}
