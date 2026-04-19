package com.daily.bread.readingplan.model;

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
@Table(name = "reading_plan_days", uniqueConstraints = @UniqueConstraint(name = "uq_reading_plan_days_plan_day_seg", columnNames = {
		"plan_id", "day_number", "segment_index" }))
public class ReadingPlanDay {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id", nullable = false)
	private ReadingPlan plan;

	@Column(name = "day_number", nullable = false)
	private Integer dayNumber;

	@Column(name = "segment_index", nullable = false)
	private Integer segmentIndex;

	@Column(name = "book_name", nullable = false, length = 256)
	private String bookName;

	@Column(name = "start_chapter", nullable = false)
	private Integer startChapter;

	@Column(name = "end_chapter", nullable = false)
	private Integer endChapter;

	@Column(name = "reading_text", nullable = false, length = 512)
	private String readingText;

	@Column(name = "completed", nullable = false)
	private boolean completed;

	public Long getId() {
		return id;
	}

	public ReadingPlan getPlan() {
		return plan;
	}

	public void setPlan(ReadingPlan plan) {
		this.plan = plan;
	}

	public Integer getDayNumber() {
		return dayNumber;
	}

	public void setDayNumber(Integer dayNumber) {
		this.dayNumber = dayNumber;
	}

	public Integer getSegmentIndex() {
		return segmentIndex;
	}

	public void setSegmentIndex(Integer segmentIndex) {
		this.segmentIndex = segmentIndex;
	}

	public String getBookName() {
		return bookName;
	}

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

	public Integer getStartChapter() {
		return startChapter;
	}

	public void setStartChapter(Integer startChapter) {
		this.startChapter = startChapter;
	}

	public Integer getEndChapter() {
		return endChapter;
	}

	public void setEndChapter(Integer endChapter) {
		this.endChapter = endChapter;
	}

	public String getReadingText() {
		return readingText;
	}

	public void setReadingText(String readingText) {
		this.readingText = readingText;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
}
