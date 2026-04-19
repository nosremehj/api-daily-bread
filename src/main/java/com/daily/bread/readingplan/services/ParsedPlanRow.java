package com.daily.bread.readingplan.services;

/**
 * Uma linha lógica do plano após o parse. Vários {@link ParsedPlanRow} podem partilhar o mesmo {@code dayNumber}
 * (segmentos 0, 1, 2…). Capítulos nulos significam o livro inteiro (resolvido na importação com base na NVI).
 */
public record ParsedPlanRow(int dayNumber, int segmentIndex, String bookName, Integer startChapter,
		Integer endChapter, String readingText) {
}
