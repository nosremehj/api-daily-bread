package com.daily.bread.readingplan.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.daily.bread.readingplan.exceptions.ReadingPlanParseException;

class ReadingPlanPdfParserTest {

	private final ReadingPlanPdfParser parser = new ReadingPlanPdfParser();

	@Test
	void parsesSingleColumnLines() {
		String text = """
				Dia Leitura Lido
				1 Gênesis 1-3
				2 Gênesis 4-6
				18 \u00caxodo 1-4
				""";
		List<ParsedPlanRow> rows = parser.parsePlainText(text);
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0).dayNumber()).isEqualTo(1);
		assertThat(rows.get(0).segmentIndex()).isEqualTo(0);
		assertThat(rows.get(0).bookName()).isEqualTo("Gênesis");
		assertThat(rows.get(0).startChapter()).isEqualTo(1);
		assertThat(rows.get(0).endChapter()).isEqualTo(3);
		assertThat(rows.get(2).bookName()).isEqualTo("\u00caxodo");
	}

	@Test
	void parsesMultipleReadingsOnOneLine_threeColumns() {
		String line = "1 Gênesis 1-3  32 Levítico 4-6  60 Números 1-3";
		List<ParsedPlanRow> rows = parser.parsePlainText(line);
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0).dayNumber()).isEqualTo(1);
		assertThat(rows.get(1).dayNumber()).isEqualTo(32);
		assertThat(rows.get(2).dayNumber()).isEqualTo(60);
	}

	@Test
	void multiWordBook() {
		List<ParsedPlanRow> rows = parser.parsePlainText("80 I Samuel 1-3");
		assertThat(rows).singleElement()
				.satisfies(d -> assertThat(d.bookName()).isEqualTo("I Samuel"));
	}

	@Test
	void footerDigitLetterGlue_doesNotCreateExtraDays() {
		String line = "1 Gênesis 1-3 1por página 2a edição 3nota";
		List<ParsedPlanRow> rows = parser.parsePlainText(line);
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).dayNumber()).isEqualTo(1);
		assertThat(rows.get(0).bookName()).isEqualTo("Gênesis");
	}

	@Test
	void lowercaseFragment_afterDayNumber_isNotParsedAsBook() {
		String text = """
				1 Gênesis 1-3
				1 ano) 1-1
				2 Gênesis 4-6
				""";
		List<ParsedPlanRow> rows = parser.parsePlainText(text);
		assertThat(rows).hasSize(2);
		assertThat(rows).extracting(ParsedPlanRow::bookName).containsExactly("Gênesis", "Gênesis");
	}

	@Test
	void mergedPdfLine_day298_doesNotSwallowLaterDays_evenWhenNextDayIsGluedToBook() {
		String line = "298 Marcos 11-13 329I Coríntios 1-4 358 II 1-1";
		List<ParsedPlanRow> rows = parser.parsePlainText(line);
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0)).satisfies(r -> {
			assertThat(r.dayNumber()).isEqualTo(298);
			assertThat(r.bookName()).isEqualTo("Marcos");
			assertThat(r.startChapter()).isEqualTo(11);
			assertThat(r.endChapter()).isEqualTo(13);
		});
		assertThat(rows.get(1)).satisfies(r -> {
			assertThat(r.dayNumber()).isEqualTo(329);
			assertThat(r.bookName()).isEqualTo("I Coríntios");
			assertThat(r.startChapter()).isEqualTo(1);
			assertThat(r.endChapter()).isEqualTo(4);
		});
		assertThat(rows.get(2)).satisfies(r -> {
			assertThat(r.dayNumber()).isEqualTo(358);
			assertThat(r.bookName()).isEqualTo("II");
			assertThat(r.startChapter()).isEqualTo(1);
			assertThat(r.endChapter()).isEqualTo(1);
		});
	}

	@Test
	void mergedPdfLine_doesNotAttachNextDayChaptersToPreviousDay() {
		String line = "274 Obadias e Jonas 305 Lucas 16-18";
		List<ParsedPlanRow> rows = parser.parsePlainText(line);
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0)).satisfies(r -> {
			assertThat(r.dayNumber()).isEqualTo(274);
			assertThat(r.bookName()).isEqualTo("Obadias");
			assertThat(r.startChapter()).isNull();
			assertThat(r.endChapter()).isNull();
		});
		assertThat(rows.get(1)).satisfies(r -> {
			assertThat(r.dayNumber()).isEqualTo(274);
			assertThat(r.bookName()).isEqualTo("Jonas");
		});
		assertThat(rows.get(2)).satisfies(r -> {
			assertThat(r.dayNumber()).isEqualTo(305);
			assertThat(r.bookName()).isEqualTo("Lucas");
			assertThat(r.startChapter()).isEqualTo(16);
			assertThat(r.endChapter()).isEqualTo(18);
		});
	}

	@Test
	void twoShortBooksSameDayPortugueseAnd_noChapterRange() {
		List<ParsedPlanRow> rows = parser.parsePlainText("274 Obadias e Jonas");
		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(r -> r.dayNumber() == 274);
		assertThat(rows).extracting(ParsedPlanRow::bookName).containsExactly("Obadias", "Jonas");
	}

	@Test
	void twoShortBooksSameDay_sofoniasEageu() {
		List<ParsedPlanRow> rows = parser.parsePlainText("279 Sofonias e Ageu");
		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(r -> r.dayNumber() == 279);
		assertThat(rows).extracting(ParsedPlanRow::bookName).containsExactly("Sofonias", "Ageu");
	}

	@Test
	void sameDayTwoBooksSemicolon_explicitChapters() {
		List<ParsedPlanRow> rows = parser.parsePlainText("349 Tito 1-3; Filemom 1-1");
		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(r -> r.dayNumber() == 349);
		assertThat(rows.get(0).segmentIndex()).isEqualTo(0);
		assertThat(rows.get(1).segmentIndex()).isEqualTo(1);
		assertThat(rows.get(0).bookName()).isEqualTo("Tito");
		assertThat(rows.get(1).bookName()).isEqualTo("Filemom");
	}

	@Test
	void sameDayTwoBooksSemicolon_secondBookWhole() {
		List<ParsedPlanRow> rows = parser.parsePlainText("349 Tito 1-3; Filemom");
		assertThat(rows).hasSize(2);
		assertThat(rows.get(1).bookName()).isEqualTo("Filemom");
		assertThat(rows.get(1).startChapter()).isNull();
		assertThat(rows.get(1).endChapter()).isNull();
	}

	@Test
	void sameDayThreeShortEpistlesWholeBooks() {
		List<ParsedPlanRow> rows = parser.parsePlainText("358 II João; III João; Judas");
		assertThat(rows).hasSize(3);
		assertThat(rows).allMatch(r -> r.dayNumber() == 358);
		assertThat(rows.get(0).readingText()).isEqualTo("II João");
		assertThat(rows.get(2).bookName()).isEqualTo("Judas");
	}

	@Test
	void sameDayPlusSeparator() {
		List<ParsedPlanRow> rows = parser.parsePlainText("40 Tito 1-2 + Filemom");
		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).bookName()).isEqualTo("Tito");
		assertThat(rows.get(1).bookName()).isEqualTo("Filemom");
	}

	@Test
	void allowsSameDayTwoLinesBecomeTwoSegments() {
		List<ParsedPlanRow> rows = parser.parsePlainText("5 Gênesis 1-3\n5 \u00caxodo 1-2");
		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).segmentIndex()).isEqualTo(0);
		assertThat(rows.get(1).segmentIndex()).isEqualTo(1);
	}

	@Test
	void rejectsWhenNothingMatches() {
		assertThatThrownBy(() -> parser.parsePlainText("apenas texto sem padrão")).isInstanceOf(ReadingPlanParseException.class);
	}

	@Test
	void readingSegmentsParseSemicolonSeparated() {
		assertThat(ReadingPlanReadingSegments.parse("Tito 1-3; Filemom 1-1")).hasSize(2)
				.extracting(ReadingPlanReadingSegments.Segment::bookName).containsExactly("Tito", "Filemom");
	}

	@Test
	void parsesD12ImportFile_distinctDays365_extraSegments() throws Exception {
		Path file = Path.of("docs/planos/plano-biblico-d12-2026-importavel.txt");
		Assumptions.assumeTrue(Files.isRegularFile(file),
				"Arquivo D12 ausente em docs/planos/ (commit ou gere com normalize_reading_plan_extract.py)");
		String text = Files.readString(file);
		List<ParsedPlanRow> rows = parser.parsePlainText(text);
		long distinct = rows.stream().mapToInt(ParsedPlanRow::dayNumber).distinct().count();
		assertThat(distinct).isEqualTo(365);
		assertThat(rows.size()).isGreaterThanOrEqualTo(365);
		ParsedPlanRow d179 = rows.stream().filter(r -> r.dayNumber() == 179).findFirst().orElseThrow();
		assertThat(d179.bookName()).isEqualTo("Salmos");
		assertThat(d179.startChapter()).isEqualTo(119);
		List<ParsedPlanRow> d349 = rows.stream().filter(r -> r.dayNumber() == 349).toList();
		assertThat(d349).hasSize(2);
		assertThat(d349.get(0).bookName()).isEqualTo("Tito");
		assertThat(d349.get(1).bookName()).isEqualTo("Filemom");
		List<ParsedPlanRow> d358 = rows.stream().filter(r -> r.dayNumber() == 358).toList();
		assertThat(d358).hasSize(3);
		List<ParsedPlanRow> d279 = rows.stream().filter(r -> r.dayNumber() == 279).toList();
		assertThat(d279).hasSize(2);
		assertThat(d279.get(0).bookName()).isEqualTo("Sofonias");
		assertThat(d279.get(1).bookName()).isEqualTo("Ageu");
	}
}
