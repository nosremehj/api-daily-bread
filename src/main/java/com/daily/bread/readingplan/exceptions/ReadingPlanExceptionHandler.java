package com.daily.bread.readingplan.exceptions;

import java.util.Map;

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
}
