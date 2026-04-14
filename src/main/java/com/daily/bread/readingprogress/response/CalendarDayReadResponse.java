package com.daily.bread.readingprogress.response;

import java.time.LocalDate;

public record CalendarDayReadResponse(LocalDate date, boolean read) {
}
