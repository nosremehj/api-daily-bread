package com.daily.bread.auth.exceptions;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException(String message) {
		super(message);
	}
}
