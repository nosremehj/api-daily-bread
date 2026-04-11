package com.daily.bread.readingplan.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

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
				18 Êxodo 1-4
				""";
		List<ParsedReadingDay> days = parser.parsePlainText(text);
		assertThat(days).hasSize(3);
		assertThat(days.get(0).dayNumber()).isEqualTo(1);
		assertThat(days.get(0).bookName()).isEqualTo("Gênesis");
		assertThat(days.get(0).startChapter()).isEqualTo(1);
		assertThat(days.get(0).endChapter()).isEqualTo(3);
		assertThat(days.get(2).bookName()).isEqualTo("Êxodo");
	}

	@Test
	void parsesMultipleReadingsOnOneLine_threeColumns() {
		String line = "1 Gênesis 1-3  32 Levítico 4-6  60 Números 1-3";
		List<ParsedReadingDay> days = parser.parsePlainText(line);
		assertThat(days).hasSize(3);
		assertThat(days.get(0).dayNumber()).isEqualTo(1);
		assertThat(days.get(1).dayNumber()).isEqualTo(32);
		assertThat(days.get(2).dayNumber()).isEqualTo(60);
	}

	@Test
	void multiWordBook() {
		List<ParsedReadingDay> days = parser.parsePlainText("80 I Samuel 1-3");
		assertThat(days).singleElement()
				.satisfies(d -> assertThat(d.bookName()).isEqualTo("I Samuel"));
	}

	@Test
	void rejectsDuplicateDayWithDifferentReading() {
		String text = "5 Gênesis 1-3\n5 Êxodo 1-2";
		assertThatThrownBy(() -> parser.parsePlainText(text)).isInstanceOf(ReadingPlanParseException.class)
				.hasMessageContaining("duplicado");
	}

	@Test
	void rejectsWhenNothingMatches() {
		assertThatThrownBy(() -> parser.parsePlainText("apenas texto sem padrão")).isInstanceOf(ReadingPlanParseException.class);
	}
}
