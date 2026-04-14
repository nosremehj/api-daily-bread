package com.daily.bread.readingprogress.exceptions;

public class EnrollmentAlreadyExistsException extends RuntimeException {

	public EnrollmentAlreadyExistsException() {
		super("Você já possui um plano ativo. Encerre ou troque o plano antes de criar outro.");
	}
}
