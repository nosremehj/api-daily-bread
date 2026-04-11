package com.daily.bread.auth.exceptions;

public class UsernameAlreadyExistsException extends RuntimeException {

	public UsernameAlreadyExistsException(String message) {
		super(message);
	}
}
