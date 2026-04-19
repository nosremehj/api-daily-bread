package com.daily.bread.readingprogress.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.daily.bread.readingprogress.exceptions.NoActiveEnrollmentException;
import com.daily.bread.readingprogress.request.CatchUpBatchDateRangesRequest;
import com.daily.bread.readingprogress.request.CatchUpDateRangeRequest;
import com.daily.bread.readingprogress.request.EnrollReadingPlanRequest;
import com.daily.bread.readingprogress.request.MarkDayReadRequest;
import com.daily.bread.readingprogress.response.CalendarDayReadResponse;
import com.daily.bread.readingprogress.response.EnrollmentSummaryResponse;
import com.daily.bread.readingprogress.response.ReadingProgressDashboardResponse;
import com.daily.bread.readingprogress.response.ReadingStatisticsResponse;
import com.daily.bread.readingprogress.response.TodayBibleReadingResponse;
import com.daily.bread.readingprogress.services.ReadingProgressService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reading-progress")
public class ReadingProgressController {

	private final ReadingProgressService readingProgressService;

	public ReadingProgressController(ReadingProgressService readingProgressService) {
		this.readingProgressService = readingProgressService;
	}

	@PostMapping("/enrollment")
	@ResponseStatus(HttpStatus.CREATED)
	public EnrollmentSummaryResponse enroll(Authentication authentication,
			@Valid @RequestBody EnrollReadingPlanRequest request) {
		return readingProgressService.enroll(authentication.getName(), request);
	}

	@DeleteMapping("/enrollment")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteEnrollment(Authentication authentication) {
		readingProgressService.deleteEnrollment(authentication.getName());
	}

	@GetMapping("/enrollment")
	public EnrollmentSummaryResponse getEnrollment(Authentication authentication) {
		return readingProgressService.getEnrollment(authentication.getName()).orElseThrow(NoActiveEnrollmentException::new);
	}

	@GetMapping("/dashboard")
	public ReadingProgressDashboardResponse dashboard(Authentication authentication,
			@RequestParam(required = false) LocalDate date) {
		return readingProgressService.dashboard(authentication.getName(), date);
	}

	@GetMapping("/today/bible")
	public TodayBibleReadingResponse todayBible(Authentication authentication,
			@RequestParam(required = false) String version, @RequestParam(required = false) LocalDate date) {
		return readingProgressService.todayBible(authentication.getName(), version, date);
	}

	@GetMapping("/calendar")
	public List<CalendarDayReadResponse> calendar(Authentication authentication,
			@RequestParam LocalDate from, @RequestParam LocalDate to) {
		return readingProgressService.calendar(authentication.getName(), from, to);
	}

	@GetMapping("/calendar/year")
	public List<CalendarDayReadResponse> calendarYear(Authentication authentication, @RequestParam int year) {
		return readingProgressService.calendarYear(authentication.getName(), year);
	}

	@GetMapping("/statistics")
	public ReadingStatisticsResponse statistics(Authentication authentication,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
		return readingProgressService.statistics(authentication.getName(), from, to);
	}

	@PostMapping("/days/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markRead(Authentication authentication, @Valid @RequestBody MarkDayReadRequest request) {
		readingProgressService.markDayRead(authentication.getName(), request);
	}

	@DeleteMapping("/days/{dayNumber}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unmarkRead(Authentication authentication, @PathVariable int dayNumber) {
		readingProgressService.unmarkDayRead(authentication.getName(), dayNumber);
	}

	@PostMapping("/catch-up/date-range")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void catchUpRange(Authentication authentication, @Valid @RequestBody CatchUpDateRangeRequest request) {
		readingProgressService.catchUpByDateRange(authentication.getName(), request);
	}

	@PostMapping("/catch-up/date-ranges")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void catchUpRanges(Authentication authentication,
			@Valid @RequestBody CatchUpBatchDateRangesRequest request) {
		readingProgressService.catchUpByDateRanges(authentication.getName(), request);
	}
}
