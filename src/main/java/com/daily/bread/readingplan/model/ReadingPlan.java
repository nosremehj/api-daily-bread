package com.daily.bread.readingplan.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "reading_plans")
public class ReadingPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "original_filename", nullable = false, length = 512)
	private String originalFilename;

	@Lob
	@Column(name = "pdf_content", nullable = false, columnDefinition = "BLOB")
	private byte[] pdfContent;

	@Column(name = "imported_at", nullable = false)
	private Instant importedAt;

	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("dayNumber ASC")
	private List<ReadingPlanDay> days = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public byte[] getPdfContent() {
		return pdfContent;
	}

	public void setPdfContent(byte[] pdfContent) {
		this.pdfContent = pdfContent;
	}

	public Instant getImportedAt() {
		return importedAt;
	}

	public void setImportedAt(Instant importedAt) {
		this.importedAt = importedAt;
	}

	public List<ReadingPlanDay> getDays() {
		return days;
	}

	public void addDay(ReadingPlanDay day) {
		days.add(day);
		day.setPlan(this);
	}
}
