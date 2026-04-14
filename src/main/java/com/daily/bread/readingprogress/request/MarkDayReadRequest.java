package com.daily.bread.readingprogress.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record MarkDayReadRequest(@NotNull Integer dayNumber, LocalDate readDate) {
}
