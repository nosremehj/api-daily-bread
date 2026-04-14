package com.daily.bread.readingprogress.exceptions;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.daily.bread.readingplan.exceptions.ReadingPlanNotFoundException;

@RestControllerAdvice(basePackages = "com.daily.bread.readingprogress.controllers")
public class ReadingProgressExceptionHandler {

	@ExceptionHandler(ReadingPlanNotFoundException.class)
	public ResponseEntity<Map<String, String>> handlePlanNotFound(ReadingPlanNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(NoActiveEnrollmentException.class)
	public ResponseEntity<Map<String, String>> handleNoEnrollment(NoActiveEnrollmentException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(EnrollmentAlreadyExistsException.class)
	public ResponseEntity<Map<String, String>> handleAlreadyEnrolled(EnrollmentAlreadyExistsException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(InvalidProgressDateException.class)
	public ResponseEntity<Map<String, String>> handleInvalidDate(InvalidProgressDateException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}
}
