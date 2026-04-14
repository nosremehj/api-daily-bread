package com.daily.bread.readingprogress.exceptions;

public class NoActiveEnrollmentException extends RuntimeException {

	public NoActiveEnrollmentException() {
		super("Nenhum plano de leitura ativo. Escolha um plano para acompanhar seu progresso.");
	}
}
