package com.daily.bread.favorites.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddVerseFavoriteRequest(@NotBlank String versionId, @NotNull @Min(1) @Max(66) Integer bookNumber,
		@NotNull @Min(1) Integer chapterNumber, @NotNull @Min(1) Integer verseNumber,
		@NotNull LocalDate readingDate) {
}
