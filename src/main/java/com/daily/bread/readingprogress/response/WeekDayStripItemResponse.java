package com.daily.bread.readingprogress.response;

import java.time.LocalDate;

/**
 * @param readWithDelay {@code true} quando houve leitura registrada nesse dia civil, mas não a do plano prevista
 *            para a data (sem leitura “no prazo” nesse dia).
 */
public record WeekDayStripItemResponse(LocalDate date, int dayOfMonth, String weekdayLabel, WeekStripDayStatus status,
		boolean readWithDelay) {
}
