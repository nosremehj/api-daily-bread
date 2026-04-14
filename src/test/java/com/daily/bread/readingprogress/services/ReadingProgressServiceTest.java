package com.daily.bread.readingprogress.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ReadingProgressServiceTest {

	@Test
	void currentStreakCountsBackwardFromTodayWhenReadToday() {
		LocalDate today = LocalDate.of(2026, 4, 10);
		Set<LocalDate> dates = Set.of(LocalDate.of(2026, 4, 8), LocalDate.of(2026, 4, 9), LocalDate.of(2026, 4, 10));
		assertThat(ReadingProgressService.currentStreakDays(dates, today)).isEqualTo(3);
	}

	@Test
	void currentStreakUsesYesterdayWhenTodayMissing() {
		LocalDate today = LocalDate.of(2026, 4, 10);
		Set<LocalDate> dates = Set.of(LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 8), LocalDate.of(2026, 4, 9));
		assertThat(ReadingProgressService.currentStreakDays(dates, today)).isEqualTo(3);
	}

	@Test
	void currentStreakZeroWhenTodayAndYesterdayMissing() {
		LocalDate today = LocalDate.of(2026, 4, 10);
		Set<LocalDate> dates = Set.of(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 6));
		assertThat(ReadingProgressService.currentStreakDays(dates, today)).isZero();
	}

	@Test
	void currentStreakOneWhenTodayReadButYesterdayMissing() {
		LocalDate today = LocalDate.of(2026, 4, 10);
		Set<LocalDate> dates = Set.of(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 8));
		assertThat(ReadingProgressService.currentStreakDays(dates, today)).isEqualTo(1);
	}

	@Test
	void dayNumberForPlanDate() {
		LocalDate start = LocalDate.of(2026, 1, 1);
		assertThat(ReadingProgressService.dayNumberForPlanDate(start, LocalDate.of(2026, 1, 1))).isEqualTo(1);
		assertThat(ReadingProgressService.dayNumberForPlanDate(start, LocalDate.of(2026, 1, 10))).isEqualTo(10);
	}
}
