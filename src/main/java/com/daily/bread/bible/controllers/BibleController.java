package com.daily.bread.bible.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.daily.bread.bible.response.BibleBookResponse;
import com.daily.bread.bible.response.BibleChapterResponse;
import com.daily.bread.bible.response.BibleVerseCompareResponse;
import com.daily.bread.bible.response.BibleVerseDetailResponse;
import com.daily.bread.bible.response.BibleVersionResponse;
import com.daily.bread.bible.services.BibleService;

@RestController
@RequestMapping("/api/v1/bible")
public class BibleController {

	private final BibleService bibleService;

	public BibleController(BibleService bibleService) {
		this.bibleService = bibleService;
	}

	@GetMapping("/versions")
	public List<BibleVersionResponse> versions() {
		return bibleService.listVersions();
	}

	@GetMapping("/{version}/books")
	public List<BibleBookResponse> books(@PathVariable String version) {
		return bibleService.listBooks(version);
	}

	@GetMapping("/verse/compare")
	public BibleVerseCompareResponse compareVerse(@RequestParam int book, @RequestParam int chapter,
			@RequestParam int verse) {
		return bibleService.compareVerse(book, chapter, verse);
	}

	@GetMapping("/{version}/books/{book}/chapters/{chapter}")
	public BibleChapterResponse chapter(
			@PathVariable String version,
			@PathVariable int book,
			@PathVariable int chapter) {
		return bibleService.getChapter(version, book, chapter);
	}

	@GetMapping("/{version}/books/{book}/chapters/{chapter}/verses/{verse}")
	public BibleVerseDetailResponse verse(@PathVariable String version, @PathVariable int book,
			@PathVariable int chapter, @PathVariable int verse) {
		return bibleService.getVerse(version, book, chapter, verse);
	}
}
