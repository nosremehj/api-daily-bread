package com.daily.bread.auth.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {

	public EmailAlreadyExistsException(String message) {
		super(message);
	}
}
