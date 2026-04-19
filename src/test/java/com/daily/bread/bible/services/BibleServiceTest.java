package com.daily.bread.bible.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.daily.bread.bible.response.BibleBookResponse;
import com.daily.bread.bible.response.BibleChapterResponse;
import com.daily.bread.bible.response.BibleVerseCompareResponse;
import com.daily.bread.bible.response.BibleVerseDetailResponse;

@SpringBootTest
class BibleServiceTest {

	@Autowired
	private BibleService bibleService;

	@Autowired
	private BibleBookResolver bibleBookResolver;

	@Test
	void resolveBookNumber_romanNumeralAliasesMatchNviDigitNames() {
		assertThat(bibleBookResolver.resolveBookNumber("I João")).hasValue(62);
		assertThat(bibleBookResolver.resolveBookNumber("II João")).hasValue(63);
		assertThat(bibleBookResolver.resolveBookNumber("III João")).hasValue(64);
		assertThat(bibleBookResolver.resolveBookNumber("I Samuel")).hasValue(9);
	}

	@Test
	void getChapterCount_matchesNviJonah() {
		assertThat(bibleService.getChapterCount("nvi", 32)).isEqualTo(4);
	}

	@Test
	void listBooks_includesChapterCountPerBook() {
		List<BibleBookResponse> books = bibleService.listBooks("nvi");
		assertThat(books).hasSize(66);
		BibleBookResponse genesis = books.get(0);
		assertThat(genesis.number()).isEqualTo(1);
		assertThat(genesis.chapterCount()).isEqualTo(bibleService.getChapterCount("nvi", 1));
	}

	@Test
	void getChapter_loadsNtlhGenesis1() {
		BibleChapterResponse ch = bibleService.getChapter("ntlh", 1, 1);
		assertThat(ch.versionId()).isEqualTo("ntlh");
		assertThat(ch.bookNumber()).isEqualTo(1);
		assertThat(ch.chapter()).isEqualTo(1);
		assertThat(ch.verses()).isNotEmpty();
		assertThat(ch.verses().get(0).verse()).isEqualTo(1);
		assertThat(ch.verses().get(0).text()).containsIgnoringCase("começo");
	}

	@Test
	void getChapter_loadsNviGenesis1() {
		BibleChapterResponse ch = bibleService.getChapter("nvi", 1, 1);
		assertThat(ch.versionId()).isEqualTo("nvi");
		assertThat(ch.verses()).isNotEmpty();
	}

	@Test
	void getVerse_returnsNtlhGenesis1_1() {
		BibleVerseDetailResponse v = bibleService.getVerse("ntlh", 1, 1, 1);
		assertThat(v.versionId()).isEqualTo("ntlh");
		assertThat(v.chapter()).isEqualTo(1);
		assertThat(v.verse()).isEqualTo(1);
		assertThat(v.text()).containsIgnoringCase("começo");
	}

	@Test
	void compareVerse_returnsThreeVersions() {
		BibleVerseCompareResponse c = bibleService.compareVerse(1, 1, 1);
		assertThat(c.chapter()).isEqualTo(1);
		assertThat(c.verse()).isEqualTo(1);
		assertThat(c.versions()).hasSize(3);
		assertThat(c.versions().stream().map(v -> v.versionId()).distinct()).hasSize(3);
	}
}
