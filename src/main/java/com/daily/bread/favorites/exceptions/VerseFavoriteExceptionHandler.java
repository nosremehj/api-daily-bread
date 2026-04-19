package com.daily.bread.favorites.exceptions;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.daily.bread.bible.exceptions.BibleNotFoundException;

@RestControllerAdvice(basePackages = "com.daily.bread.favorites.controllers")
public class VerseFavoriteExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(" "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
	}

	@ExceptionHandler(VerseFavoriteDuplicateException.class)
	public ResponseEntity<Map<String, String>> handleDuplicate(VerseFavoriteDuplicateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(VerseFavoriteNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(VerseFavoriteNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(BibleNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleBibleNotFound(BibleNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("error", "Este versículo já está favoritado nesta data de leitura."));
	}
}
