package com.daily.bread.bible.exceptions;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.daily.bread.bible.controllers")
public class BibleExceptionHandler {

	@ExceptionHandler(BibleNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(BibleNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}
}
