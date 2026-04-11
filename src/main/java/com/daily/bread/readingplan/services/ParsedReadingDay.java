package com.daily.bread.readingplan.services;

public record ParsedReadingDay(int dayNumber, String bookName, int startChapter, int endChapter, String readingText) {
}
