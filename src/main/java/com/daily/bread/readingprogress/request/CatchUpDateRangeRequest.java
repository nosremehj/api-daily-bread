package com.daily.bread.readingprogress.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CatchUpDateRangeRequest(@NotNull LocalDate fromInclusive, @NotNull LocalDate toInclusive) {
}
