package com.daily.bread.readingprogress.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_reading_completions", uniqueConstraints = @UniqueConstraint(name = "uq_user_reading_completions_enrollment_day", columnNames = {
		"enrollment_id", "day_number" }))
public class UserReadingCompletion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "enrollment_id", nullable = false)
	private UserReadingEnrollment enrollment;

	@Column(name = "day_number", nullable = false)
	private int dayNumber;

	@Column(name = "read_date", nullable = false)
	private LocalDate readDate;

	public Long getId() {
		return id;
	}

	public UserReadingEnrollment getEnrollment() {
		return enrollment;
	}

	public void setEnrollment(UserReadingEnrollment enrollment) {
		this.enrollment = enrollment;
	}

	public int getDayNumber() {
		return dayNumber;
	}

	public void setDayNumber(int dayNumber) {
		this.dayNumber = dayNumber;
	}

	public LocalDate getReadDate() {
		return readDate;
	}

	public void setReadDate(LocalDate readDate) {
		this.readDate = readDate;
	}
}
