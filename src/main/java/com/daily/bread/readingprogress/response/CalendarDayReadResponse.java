package com.daily.bread.readingprogress.response;

import java.time.LocalDate;

/**
 * @param readWithDelay {@code true} quando houve registro de leitura na data civil ({@code read}) mas não no dia
 *            agendado do plano para essa data (leitura apenas com atraso / outro dia do plano na mesma data).
 */
public record CalendarDayReadResponse(LocalDate date, boolean read, boolean readWithDelay) {
}
