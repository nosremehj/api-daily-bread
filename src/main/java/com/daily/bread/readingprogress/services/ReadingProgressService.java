package com.daily.bread.readingprogress.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daily.bread.auth.model.User;
import com.daily.bread.bible.response.BibleChapterResponse;
import com.daily.bread.bible.services.BibleBookResolver;
import com.daily.bread.bible.services.BibleService;
import com.daily.bread.auth.repository.UserRepository;
import com.daily.bread.readingplan.exceptions.ReadingPlanNotFoundException;
import com.daily.bread.readingplan.model.ReadingPlan;
import com.daily.bread.readingplan.model.ReadingPlanDay;
import com.daily.bread.readingplan.repository.ReadingPlanDayRepository;
import com.daily.bread.readingplan.repository.ReadingPlanRepository;
import com.daily.bread.readingprogress.exceptions.EnrollmentAlreadyExistsException;
import com.daily.bread.readingprogress.exceptions.InvalidProgressDateException;
import com.daily.bread.readingprogress.exceptions.NoActiveEnrollmentException;
import com.daily.bread.readingprogress.model.UserReadingCompletion;
import com.daily.bread.readingprogress.model.UserReadingEnrollment;
import com.daily.bread.readingprogress.repository.UserReadingCompletionRepository;
import com.daily.bread.readingprogress.repository.UserReadingEnrollmentRepository;
import com.daily.bread.readingprogress.request.CatchUpDateRangeRequest;
import com.daily.bread.readingprogress.request.EnrollReadingPlanRequest;
import com.daily.bread.readingprogress.request.MarkDayReadRequest;
import com.daily.bread.readingprogress.response.CalendarDayReadResponse;
import com.daily.bread.readingprogress.response.EnrollmentSummaryResponse;
import com.daily.bread.readingprogress.response.ReadingProgressDashboardResponse;
import com.daily.bread.readingprogress.response.TodayBibleBlockResponse;
import com.daily.bread.readingprogress.response.TodayBibleReadingResponse;
import com.daily.bread.readingprogress.response.TodayReadingBlockResponse;
import com.daily.bread.readingprogress.response.TodayReadingSectionResponse;
import com.daily.bread.readingprogress.response.WeekDayStripItemResponse;
import com.daily.bread.readingprogress.response.WeekStripDayStatus;

@Service
public class ReadingProgressService {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final UserRepository userRepository;
	private final ReadingPlanRepository planRepository;
	private final ReadingPlanDayRepository planDayRepository;
	private final UserReadingEnrollmentRepository enrollmentRepository;
	private final UserReadingCompletionRepository completionRepository;
	private final BibleService bibleService;
	private final BibleBookResolver bibleBookResolver;

	public ReadingProgressService(UserRepository userRepository, ReadingPlanRepository planRepository,
			ReadingPlanDayRepository planDayRepository, UserReadingEnrollmentRepository enrollmentRepository,
			UserReadingCompletionRepository completionRepository, BibleService bibleService,
			BibleBookResolver bibleBookResolver) {
		this.userRepository = userRepository;
		this.planRepository = planRepository;
		this.planDayRepository = planDayRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.completionRepository = completionRepository;
		this.bibleService = bibleService;
		this.bibleBookResolver = bibleBookResolver;
	}

	@Transactional
	public EnrollmentSummaryResponse enroll(String username, EnrollReadingPlanRequest request) {
		User user = user(username);
		if (enrollmentRepository.findByUser_Id(user.getId()).isPresent()) {
			throw new EnrollmentAlreadyExistsException();
		}
		ReadingPlan plan = planRepository.findById(request.planId())
				.orElseThrow(() -> new ReadingPlanNotFoundException(request.planId()));
		long totalDays = planDayRepository.countByPlan_Id(plan.getId());
		if (totalDays == 0) {
			throw new InvalidProgressDateException("O plano selecionado não possui dias de leitura.");
		}
		UserReadingEnrollment enrollment = new UserReadingEnrollment();
		enrollment.setUser(user);
		enrollment.setPlan(plan);
		enrollment.setPlanStartDate(request.planStartDate());
		enrollment.setCreatedAt(Instant.now());
		UserReadingEnrollment saved = enrollmentRepository.save(enrollment);
		if (request.catchUpThroughDate() != null) {
			catchUpThroughScheduledDate(saved, request.planStartDate(), request.catchUpThroughDate(), (int) totalDays);
		}
		return new EnrollmentSummaryResponse(saved.getId(), plan.getId(), plan.getOriginalFilename(),
				saved.getPlanStartDate(), saved.getCreatedAt());
	}

	@Transactional
	public void deleteEnrollment(String username) {
		User user = user(username);
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user.getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		enrollmentRepository.delete(enrollment);
	}

	@Transactional(readOnly = true)
	public Optional<EnrollmentSummaryResponse> getEnrollment(String username) {
		User user = user(username);
		return enrollmentRepository.findByUser_Id(user.getId()).map(this::toSummary);
	}

	@Transactional(readOnly = true)
	public ReadingProgressDashboardResponse dashboard(String username, LocalDate referenceDate) {
		LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		ReadingPlan plan = enrollment.getPlan();
		long totalDaysLong = planDayRepository.countByPlan_Id(plan.getId());
		int totalDays = (int) totalDaysLong;
		long completedDays = completionRepository.countByEnrollment_Id(enrollment.getId());
		int percent = totalDays == 0 ? 0 : (int) Math.round(100.0 * completedDays / totalDays);
		Set<LocalDate> allReadDates = completionRepository.findAllDistinctReadDates(enrollment.getId());
		int streak = currentStreakDays(allReadDates, ref);
		int daysLeftInYear = (int) ChronoUnit.DAYS.between(ref, ref.with(TemporalAdjusters.lastDayOfYear()));
		LocalDate weekStart = ref.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
		LocalDate weekEnd = weekStart.plusDays(6);
		Set<LocalDate> weekReads = completionRepository.findDistinctReadDatesBetween(enrollment.getId(), weekStart,
				weekEnd);
		List<WeekDayStripItemResponse> strip = buildWeekStrip(weekStart, ref, weekReads);
		TodayReadingSectionResponse today = buildTodaySection(enrollment, plan.getId(), totalDays, ref);
		return new ReadingProgressDashboardResponse(plan.getId(), plan.getOriginalFilename(), totalDays,
				(int) completedDays, percent, streak, daysLeftInYear, strip, today);
	}

	@Transactional(readOnly = true)
	public TodayBibleReadingResponse todayBible(String username, String versionId, LocalDate ref) {
		LocalDate referenceDate = ref != null ? ref : LocalDate.now();
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		String v = bibleService.requireVersion(
				versionId == null || versionId.isBlank() ? "nvi" : versionId.trim());
		ReadingPlan plan = enrollment.getPlan();
		int totalDays = (int) planDayRepository.countByPlan_Id(plan.getId());
		LocalDate planStart = enrollment.getPlanStartDate();
		int scheduledDay = dayNumberForPlanDate(planStart, referenceDate);
		if (scheduledDay < 1 || scheduledDay > totalDays) {
			return new TodayBibleReadingResponse(referenceDate, null, null, v, false, List.of());
		}
		LocalDate scheduledDate = scheduledDateForDay(planStart, scheduledDay);
		Optional<ReadingPlanDay> dayOpt = planDayRepository.findByPlan_IdAndDayNumber(plan.getId(), scheduledDay);
		boolean done = completionRepository.existsByEnrollment_IdAndDayNumber(enrollment.getId(), scheduledDay);
		if (dayOpt.isEmpty()) {
			return new TodayBibleReadingResponse(referenceDate, scheduledDay, scheduledDate, v, done, List.of());
		}
		ReadingPlanDay d = dayOpt.get();
		Integer bookNum = bibleBookResolver.resolveBookNumber(d.getBookName()).orElse(null);
		String abbrev = bookNum != null ? bibleBookResolver.abbrevForBook(bookNum).orElse(null) : null;
		List<BibleChapterResponse> chapters = new ArrayList<>();
		if (bookNum != null) {
			for (int ch = d.getStartChapter(); ch <= d.getEndChapter(); ch++) {
				chapters.add(bibleService.getChapter(v, bookNum, ch));
			}
		}
		TodayBibleBlockResponse block = new TodayBibleBlockResponse(d.getId(), d.getDayNumber(), d.getBookName(),
				bookNum, abbrev, d.getStartChapter(), d.getEndChapter(), d.getReadingText(), done,
				List.copyOf(chapters));
		return new TodayBibleReadingResponse(referenceDate, scheduledDay, scheduledDate, v, done, List.of(block));
	}

	@Transactional(readOnly = true)
	public List<CalendarDayReadResponse> calendar(String username, LocalDate fromInclusive, LocalDate toInclusive) {
		if (fromInclusive == null || toInclusive == null) {
			throw new InvalidProgressDateException("Informe from e to (ISO-8601).");
		}
		if (toInclusive.isBefore(fromInclusive)) {
			throw new InvalidProgressDateException("O fim do intervalo não pode ser antes do início.");
		}
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		Set<LocalDate> readDates = completionRepository.findDistinctReadDatesBetween(enrollment.getId(), fromInclusive,
				toInclusive);
		List<CalendarDayReadResponse> out = new ArrayList<>();
		for (LocalDate d = fromInclusive; !d.isAfter(toInclusive); d = d.plusDays(1)) {
			out.add(new CalendarDayReadResponse(d, readDates.contains(d)));
		}
		return out;
	}

	@Transactional
	public void markDayRead(String username, MarkDayReadRequest request) {
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		int totalDays = (int) planDayRepository.countByPlan_Id(enrollment.getPlan().getId());
		int dayNumber = request.dayNumber();
		if (dayNumber < 1 || dayNumber > totalDays) {
			throw new InvalidProgressDateException(
					"Dia fora do plano. Informe um número entre 1 e " + totalDays + ".");
		}
		LocalDate readDate = request.readDate() != null ? request.readDate() : LocalDate.now();
		UserReadingCompletion row = completionRepository
				.findByEnrollment_IdAndDayNumber(enrollment.getId(), dayNumber)
				.orElseGet(UserReadingCompletion::new);
		if (row.getId() == null) {
			row.setEnrollment(enrollment);
			row.setDayNumber(dayNumber);
		}
		row.setReadDate(readDate);
		completionRepository.save(row);
	}

	@Transactional
	public void catchUpByDateRange(String username, CatchUpDateRangeRequest request) {
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		LocalDate from = request.fromInclusive();
		LocalDate to = request.toInclusive();
		if (to.isBefore(from)) {
			throw new InvalidProgressDateException("O fim do intervalo não pode ser antes do início.");
		}
		LocalDate planStart = enrollment.getPlanStartDate();
		long totalDaysLong = planDayRepository.countByPlan_Id(enrollment.getPlan().getId());
		int totalDays = (int) totalDaysLong;
		for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
			int dayNumber = dayNumberForPlanDate(planStart, d);
			if (dayNumber < 1 || dayNumber > totalDays) {
				continue;
			}
			UserReadingCompletion row = completionRepository
					.findByEnrollment_IdAndDayNumber(enrollment.getId(), dayNumber)
					.orElseGet(UserReadingCompletion::new);
			if (row.getId() == null) {
				row.setEnrollment(enrollment);
				row.setDayNumber(dayNumber);
			}
			row.setReadDate(d);
			completionRepository.save(row);
		}
	}

	private void catchUpThroughScheduledDate(UserReadingEnrollment enrollment, LocalDate planStart,
			LocalDate catchUpThroughDate, int totalDays) {
		if (catchUpThroughDate.isBefore(planStart)) {
			throw new InvalidProgressDateException("catchUpThroughDate não pode ser anterior a planStartDate.");
		}
		int lastDay = dayNumberForPlanDate(planStart, catchUpThroughDate);
		if (lastDay < 1) {
			throw new InvalidProgressDateException("Data de recuperação inválida em relação ao início do plano.");
		}
		if (lastDay > totalDays) {
			lastDay = totalDays;
		}
		IntStream.rangeClosed(1, lastDay).forEach(dayNumber -> {
			LocalDate scheduled = scheduledDateForDay(planStart, dayNumber);
			UserReadingCompletion row = completionRepository
					.findByEnrollment_IdAndDayNumber(enrollment.getId(), dayNumber)
					.orElseGet(UserReadingCompletion::new);
			if (row.getId() == null) {
				row.setEnrollment(enrollment);
				row.setDayNumber(dayNumber);
			}
			row.setReadDate(scheduled);
			completionRepository.save(row);
		});
	}

	private TodayReadingSectionResponse buildTodaySection(UserReadingEnrollment enrollment, Long planId, int totalDays,
			LocalDate ref) {
		LocalDate planStart = enrollment.getPlanStartDate();
		int scheduledDay = dayNumberForPlanDate(planStart, ref);
		if (scheduledDay < 1 || scheduledDay > totalDays) {
			return new TodayReadingSectionResponse(ref, null, null, List.of());
		}
		LocalDate scheduledDate = scheduledDateForDay(planStart, scheduledDay);
		Optional<ReadingPlanDay> day = planDayRepository.findByPlan_IdAndDayNumber(planId, scheduledDay);
		boolean done = completionRepository.existsByEnrollment_IdAndDayNumber(enrollment.getId(), scheduledDay);
		List<TodayReadingBlockResponse> blocks = day
				.map(d -> {
					Integer bookNum = bibleBookResolver.resolveBookNumber(d.getBookName()).orElse(null);
					String abbrev = bookNum != null ? bibleBookResolver.abbrevForBook(bookNum).orElse(null) : null;
					return List.of(new TodayReadingBlockResponse(d.getId(), d.getDayNumber(), d.getBookName(), bookNum,
							abbrev, d.getStartChapter(), d.getEndChapter(), d.getReadingText(), done));
				})
				.orElse(List.of());
		return new TodayReadingSectionResponse(ref, scheduledDay, scheduledDate, blocks);
	}

	private List<WeekDayStripItemResponse> buildWeekStrip(LocalDate weekStart, LocalDate today,
			Set<LocalDate> weekReads) {
		List<WeekDayStripItemResponse> items = new ArrayList<>(7);
		for (int i = 0; i < 7; i++) {
			LocalDate d = weekStart.plusDays(i);
			String label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT_STANDALONE, PT_BR);
			WeekStripDayStatus status;
			if (weekReads.contains(d)) {
				status = WeekStripDayStatus.COMPLETED;
			}
			else if (d.isAfter(today)) {
				status = WeekStripDayStatus.UPCOMING;
			}
			else if (d.equals(today)) {
				status = WeekStripDayStatus.ACTIVE;
			}
			else {
				status = WeekStripDayStatus.MISSED;
			}
			items.add(new WeekDayStripItemResponse(d, d.getDayOfMonth(), label, status));
		}
		return items;
	}

	private EnrollmentSummaryResponse toSummary(UserReadingEnrollment e) {
		return new EnrollmentSummaryResponse(e.getId(), e.getPlan().getId(), e.getPlan().getOriginalFilename(),
				e.getPlanStartDate(), e.getCreatedAt());
	}

	private User user(String username) {
		return userRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new IllegalStateException("Usuário não encontrado para o token atual."));
	}

	static int dayNumberForPlanDate(LocalDate planStart, LocalDate date) {
		return (int) ChronoUnit.DAYS.between(planStart, date) + 1;
	}

	static LocalDate scheduledDateForDay(LocalDate planStart, int dayNumber) {
		return planStart.plusDays(dayNumber - 1L);
	}

	static int currentStreakDays(Set<LocalDate> readDates, LocalDate today) {
		if (readDates.isEmpty()) {
			return 0;
		}
		LocalDate anchor;
		if (readDates.contains(today)) {
			anchor = today;
		}
		else if (readDates.contains(today.minusDays(1))) {
			anchor = today.minusDays(1);
		}
		else {
			return 0;
		}
		int streak = 0;
		for (LocalDate d = anchor; readDates.contains(d); d = d.minusDays(1)) {
			streak++;
		}
		return streak;
	}
}
