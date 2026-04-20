package com.daily.bread.readingprogress.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.daily.bread.readingprogress.model.UserReadingCompletion;

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

	@Test
	void longestStreakSingleDay() {
		assertThat(ReadingProgressService.longestStreakDays(Set.of(LocalDate.of(2026, 4, 1)))).isEqualTo(1);
	}

	@Test
	void longestStreakConsecutiveRun() {
		Set<LocalDate> dates = Set.of(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3),
				LocalDate.of(2026, 4, 10));
		assertThat(ReadingProgressService.longestStreakDays(dates)).isEqualTo(3);
	}

	@Test
	void longestStreakEmpty() {
		assertThat(ReadingProgressService.longestStreakDays(Set.of())).isZero();
	}

	@Test
	void onTimeReadCivilDates_includesOnlyWhenReadDateMatchesScheduledDay() {
		LocalDate start = LocalDate.of(2026, 1, 1);
		UserReadingCompletion onTime = completion(1, LocalDate.of(2026, 1, 1));
		UserReadingCompletion late = completion(2, LocalDate.of(2026, 1, 10));
		Set<LocalDate> onTimeDates = ReadingProgressService.onTimeReadCivilDates(start, 365,
				List.of(onTime, late));
		assertThat(onTimeDates).containsExactly(LocalDate.of(2026, 1, 1));
	}

	@Test
	void currentStreakIgnoresLateCatchUpReadDate() {
		LocalDate today = LocalDate.of(2026, 1, 10);
		UserReadingCompletion day1 = completion(1, LocalDate.of(2026, 1, 1));
		UserReadingCompletion day2Late = completion(2, LocalDate.of(2026, 1, 10));
		Set<LocalDate> onTimeOnly = ReadingProgressService.onTimeReadCivilDates(LocalDate.of(2026, 1, 1), 365,
				List.of(day1, day2Late));
		assertThat(ReadingProgressService.currentStreakDays(onTimeOnly, today)).isZero();
	}

	private static UserReadingCompletion completion(int dayNumber, LocalDate readDate) {
		UserReadingCompletion c = new UserReadingCompletion();
		c.setDayNumber(dayNumber);
		c.setReadDate(readDate);
		c.setReadWithDelay(false);
		return c;
	}

	@Test
	void onTimeReadCivilDates_excludesExplicitDelaySameReadDate() {
		LocalDate start = LocalDate.of(2026, 1, 1);
		UserReadingCompletion delayed = completion(2, LocalDate.of(2026, 1, 2));
		delayed.setReadWithDelay(true);
		Set<LocalDate> onTimeDates = ReadingProgressService.onTimeReadCivilDates(start, 365, List.of(delayed));
		assertThat(onTimeDates).isEmpty();
	}
}
