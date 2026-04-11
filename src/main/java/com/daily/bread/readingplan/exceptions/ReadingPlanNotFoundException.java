package com.daily.bread.readingplan.exceptions;

public class ReadingPlanNotFoundException extends RuntimeException {

	public ReadingPlanNotFoundException(Long id) {
		super("Plano não encontrado: " + id);
	}
}
