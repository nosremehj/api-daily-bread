package com.daily.bread.readingprogress.model;

import java.time.LocalDate;

import com.daily.bread.readingplan.model.ReadingPlanDay;

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
@Table(name = "user_reading_segment_completions", uniqueConstraints = @UniqueConstraint(name = "uq_user_reading_segment_completions_enrollment_day_seg", columnNames = {
		"enrollment_id", "reading_plan_day_id", "segment_index" }))
public class UserReadingSegmentCompletion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "enrollment_id", nullable = false)
	private UserReadingEnrollment enrollment;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "reading_plan_day_id", nullable = false)
	private ReadingPlanDay readingPlanDay;

	@Column(name = "segment_index", nullable = false)
	private int segmentIndex;

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

	public ReadingPlanDay getReadingPlanDay() {
		return readingPlanDay;
	}

	public void setReadingPlanDay(ReadingPlanDay readingPlanDay) {
		this.readingPlanDay = readingPlanDay;
	}

	public int getSegmentIndex() {
		return segmentIndex;
	}

	public void setSegmentIndex(int segmentIndex) {
		this.segmentIndex = segmentIndex;
	}

	public LocalDate getReadDate() {
		return readDate;
	}

	public void setReadDate(LocalDate readDate) {
		this.readDate = readDate;
	}
}
