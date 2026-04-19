package com.daily.bread.readingplan.exceptions;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.daily.bread.readingplan.controllers")
public class ReadingPlanExceptionHandler {

	@ExceptionHandler(ReadingPlanParseException.class)
	public ResponseEntity<Map<String, String>> handleParse(ReadingPlanParseException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(ReadingPlanNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(ReadingPlanNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(ReadingPlanDuplicateException.class)
	public ResponseEntity<Map<String, String>> handleDuplicate(ReadingPlanDuplicateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("error", ex.getMessage(), "existingPlanId", String.valueOf(ex.getExistingPlanId())));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException ex) {
		String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
		if (msg != null && msg.contains("uq_reading_plans_pdf_sha256")) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("error", "Este PDF já foi importado (plano idêntico)."));
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Erro ao gravar no banco de dados."));
	}
}
