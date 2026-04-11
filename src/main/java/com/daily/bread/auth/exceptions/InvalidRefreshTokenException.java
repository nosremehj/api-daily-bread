package com.daily.bread.auth.exceptions;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException(String message) {
		super(message);
	}
}
