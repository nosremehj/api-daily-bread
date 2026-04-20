package com.daily.bread.readingplan.services;

import java.util.List;

/**
 * Resultado de uma leitura completa do PDF: linhas parseadas + texto bruto/normalizado para depuração.
 */
public record ReadingPlanPdfExtractBundle(List<ParsedPlanRow> rows, String rawPdfText, String normalizedPdfText,
		List<String> relevantPreviewLines) {
}
