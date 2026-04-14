package com.daily.bread.readingprogress.response;

import java.time.LocalDate;

public record WeekDayStripItemResponse(LocalDate date, int dayOfMonth, String weekdayLabel, WeekStripDayStatus status) {
}
