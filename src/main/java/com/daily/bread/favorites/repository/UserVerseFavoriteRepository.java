package com.daily.bread.favorites.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.daily.bread.favorites.model.UserVerseFavorite;

public interface UserVerseFavoriteRepository extends JpaRepository<UserVerseFavorite, Long> {

	List<UserVerseFavorite> findAllByUser_IdAndReadingDateOrderByCreatedAtAsc(Long userId, LocalDate readingDate);

	Optional<UserVerseFavorite> findByIdAndUser_Id(Long id, Long userId);

	boolean existsByUser_IdAndVersionIdAndBookNumberAndChapterNumberAndVerseNumberAndReadingDate(Long userId,
			String versionId, int bookNumber, int chapterNumber, int verseNumber, LocalDate readingDate);
}
