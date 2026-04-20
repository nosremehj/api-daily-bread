package com.daily.bread.readingprogress.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/**
 * @param readingPlanDayId quando informado, marca só esse trecho (ex.: uma das leituras do dia). Quando omitido,
 *                           marca todos os trechos do dia (comportamento anterior / recuperação).
 * @param segmentIndex     obrigatório só quando há vários blocos com o mesmo {@code readingPlanDayId} (linha legada
 *                           com {@code ;} no texto); caso contrário pode ser nulo (equivale a 0).
 */
public record MarkDayReadRequest(@NotNull Integer dayNumber, LocalDate readDate, Long readingPlanDayId,
		Integer segmentIndex) {
}
