package com.daily.bread.readingprogress.response;

import java.time.LocalDate;
import java.util.List;

/**
 * @param readDatesInPeriod datas civis com qualquer registro de leitura (mesma base do calendário: {@code read_date}
 *            distinto).
 * @param daysReadOnTimeInPeriod quantidade de dias civis no período em que a leitura do plano foi feita <strong>no
 *            prazo</strong> (sem atraso explícito e {@code read_date} no dia agendado).
 * @param daysReadWithDelayInPeriod quantidade de dias civis no período com leitura registrada <strong>só com
 *            atraso</strong> (inclui {@code read_with_delay} ou {@code read_date} fora do slot).
 * @param readOnTimeDatesInPeriod lista ordenável de datas ISO acima (subconjunto de {@code periodFrom}…{@code periodTo}).
 * @param readWithDelayDatesInPeriod datas civis no período com leitura em atraso (para o dia agendado nessa data).
 * @param missedScheduledDatesInPeriod dias civis <strong>agendados</strong> até “hoje” (ou fim do período) em que não
 *            houve registro na data — equivalente ao critério de {@code daysMissedInPeriod}.
 */
public record ReadingStatisticsResponse(Long planId, String planFilename, LocalDate planStartDate, LocalDate periodFrom,
		LocalDate periodTo, int totalPlanDays, int completedDaysInPlan, int daysReadInPeriod, int daysMissedInPeriod,
		boolean hasMissedDaysInPeriod, int currentStreakDays, int longestStreakDays, int annualProgressPercent,
		List<LocalDate> readDatesInPeriod, int daysReadOnTimeInPeriod, int daysReadWithDelayInPeriod,
		List<LocalDate> readOnTimeDatesInPeriod, List<LocalDate> readWithDelayDatesInPeriod,
		List<LocalDate> missedScheduledDatesInPeriod, Integer nextMilestonePercent, Integer daysUntilNextMilestone) {
}
