package com.daily.bread.auth.exceptions;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.daily.bread.auth.controllers")
public class AuthExceptionHandler {

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<Map<String, String>> handleEmailExists(EmailAlreadyExistsException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(UsernameAlreadyExistsException.class)
	public ResponseEntity<Map<String, String>> handleUsernameExists(UsernameAlreadyExistsException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<Map<String, String>> handleInvalidRefresh(InvalidRefreshTokenException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(" "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
	}
}
