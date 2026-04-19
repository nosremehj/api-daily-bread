package com.daily.bread.favorites.model;

import java.time.Instant;
import java.time.LocalDate;

import com.daily.bread.auth.model.User;

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
@Table(name = "user_verse_favorites", uniqueConstraints = @UniqueConstraint(name = "uq_user_verse_favorites_user_ref_date", columnNames = {
		"user_id", "version_id", "book_number", "chapter_number", "verse_number", "reading_date" }))
public class UserVerseFavorite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "version_id", nullable = false, length = 16)
	private String versionId;

	@Column(name = "book_number", nullable = false)
	private int bookNumber;

	@Column(name = "chapter_number", nullable = false)
	private int chapterNumber;

	@Column(name = "verse_number", nullable = false)
	private int verseNumber;

	@Column(name = "reading_date", nullable = false)
	private LocalDate readingDate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public int getBookNumber() {
		return bookNumber;
	}

	public void setBookNumber(int bookNumber) {
		this.bookNumber = bookNumber;
	}

	public int getChapterNumber() {
		return chapterNumber;
	}

	public void setChapterNumber(int chapterNumber) {
		this.chapterNumber = chapterNumber;
	}

	public int getVerseNumber() {
		return verseNumber;
	}

	public void setVerseNumber(int verseNumber) {
		this.verseNumber = verseNumber;
	}

	public LocalDate getReadingDate() {
		return readingDate;
	}

	public void setReadingDate(LocalDate readingDate) {
		this.readingDate = readingDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
