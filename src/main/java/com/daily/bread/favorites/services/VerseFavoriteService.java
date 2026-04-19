package com.daily.bread.favorites.services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daily.bread.auth.model.User;
import com.daily.bread.auth.repository.UserRepository;
import com.daily.bread.bible.response.BibleVerseDetailResponse;
import com.daily.bread.bible.services.BibleService;
import com.daily.bread.favorites.exceptions.VerseFavoriteDuplicateException;
import com.daily.bread.favorites.exceptions.VerseFavoriteNotFoundException;
import com.daily.bread.favorites.model.UserVerseFavorite;
import com.daily.bread.favorites.repository.UserVerseFavoriteRepository;
import com.daily.bread.favorites.request.AddVerseFavoriteRequest;
import com.daily.bread.favorites.response.VerseFavoriteResponse;

@Service
public class VerseFavoriteService {

	private final UserRepository userRepository;
	private final UserVerseFavoriteRepository favoriteRepository;
	private final BibleService bibleService;

	public VerseFavoriteService(UserRepository userRepository, UserVerseFavoriteRepository favoriteRepository,
			BibleService bibleService) {
		this.userRepository = userRepository;
		this.favoriteRepository = favoriteRepository;
		this.bibleService = bibleService;
	}

	@Transactional
	public VerseFavoriteResponse add(String username, AddVerseFavoriteRequest request) {
		User user = user(username);
		String v = bibleService.requireVersion(request.versionId());
		int book = request.bookNumber();
		int chapter = request.chapterNumber();
		int verse = request.verseNumber();
		bibleService.getVerse(v, book, chapter, verse);
		if (favoriteRepository.existsByUser_IdAndVersionIdAndBookNumberAndChapterNumberAndVerseNumberAndReadingDate(
				user.getId(), v, book, chapter, verse, request.readingDate())) {
			throw new VerseFavoriteDuplicateException();
		}
		UserVerseFavorite row = new UserVerseFavorite();
		row.setUser(user);
		row.setVersionId(v);
		row.setBookNumber(book);
		row.setChapterNumber(chapter);
		row.setVerseNumber(verse);
		row.setReadingDate(request.readingDate());
		row.setCreatedAt(Instant.now());
		UserVerseFavorite saved = favoriteRepository.save(row);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<VerseFavoriteResponse> listByReadingDate(String username, LocalDate readingDate) {
		User user = user(username);
		List<UserVerseFavorite> rows = favoriteRepository.findAllByUser_IdAndReadingDateOrderByCreatedAtAsc(
				user.getId(), readingDate);
		return rows.stream().map(this::toResponse).toList();
	}

	@Transactional
	public void delete(String username, Long id) {
		User user = user(username);
		UserVerseFavorite row = favoriteRepository.findByIdAndUser_Id(id, user.getId())
				.orElseThrow(VerseFavoriteNotFoundException::new);
		favoriteRepository.delete(row);
	}

	private VerseFavoriteResponse toResponse(UserVerseFavorite f) {
		BibleVerseDetailResponse detail = bibleService.getVerse(f.getVersionId(), f.getBookNumber(),
				f.getChapterNumber(), f.getVerseNumber());
		return new VerseFavoriteResponse(f.getId(), f.getVersionId(), f.getBookNumber(), detail.abbrev(),
				detail.bookName(), f.getChapterNumber(), f.getVerseNumber(), detail.text(), f.getReadingDate(),
				f.getCreatedAt());
	}

	private User user(String username) {
		return userRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new IllegalStateException("Usuário não encontrado para o token atual."));
	}
}
