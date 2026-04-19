package com.daily.bread.readingplan.exceptions;

public class ReadingPlanDuplicateException extends RuntimeException {

	private final Long existingPlanId;

	public ReadingPlanDuplicateException(Long existingPlanId) {
		super("Este PDF já foi importado (plano idêntico). Use o plano existente.");
		this.existingPlanId = existingPlanId;
	}

	public Long getExistingPlanId() {
		return existingPlanId;
	}
}
