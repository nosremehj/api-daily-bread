package com.daily.bread.readingplan.exceptions;

public class ReadingPlanParseException extends RuntimeException {

	public ReadingPlanParseException(String message) {
		super(message);
	}

	public ReadingPlanParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
