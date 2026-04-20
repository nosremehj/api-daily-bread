package com.daily.bread.readingprogress.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
import com.daily.bread.readingplan.services.ReadingPlanReadingSegments;
import com.daily.bread.readingprogress.exceptions.EnrollmentAlreadyExistsException;
import com.daily.bread.readingprogress.exceptions.InvalidProgressDateException;
import com.daily.bread.readingprogress.exceptions.NoActiveEnrollmentException;
import com.daily.bread.readingprogress.model.UserReadingCompletion;
import com.daily.bread.readingprogress.model.UserReadingEnrollment;
import com.daily.bread.readingprogress.model.UserReadingSegmentCompletion;
import com.daily.bread.readingprogress.repository.UserReadingCompletionRepository;
import com.daily.bread.readingprogress.repository.UserReadingEnrollmentRepository;
import com.daily.bread.readingprogress.repository.UserReadingSegmentCompletionRepository;
import com.daily.bread.readingprogress.request.CatchUpBatchDateRangesRequest;
import com.daily.bread.readingprogress.request.CatchUpDateRangeRequest;
import com.daily.bread.readingprogress.request.EnrollReadingPlanRequest;
import com.daily.bread.readingprogress.request.MarkDayReadRequest;
import com.daily.bread.readingprogress.response.CalendarDayReadResponse;
import com.daily.bread.readingprogress.response.EnrollmentSummaryResponse;
import com.daily.bread.readingprogress.response.ReadingProgressDashboardResponse;
import com.daily.bread.readingprogress.response.ReadingStatisticsResponse;
import com.daily.bread.readingprogress.response.TodayBibleBlockResponse;
import com.daily.bread.readingprogress.response.TodayBibleReadingResponse;
import com.daily.bread.readingprogress.response.TodayReadingBlockResponse;
import com.daily.bread.readingprogress.response.TodayReadingSectionResponse;
import com.daily.bread.readingprogress.response.WeekDayStripItemResponse;
import com.daily.bread.readingprogress.response.WeekStripDayStatus;

@Service
public class ReadingProgressService {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	/** Inclusive calendar span limit for {@code /calendar} (one civil year, leap inclusive). */
	private static final int MAX_CALENDAR_RANGE_DAYS = 366;

	private final UserRepository userRepository;
	private final ReadingPlanRepository planRepository;
	private final ReadingPlanDayRepository planDayRepository;
	private final UserReadingEnrollmentRepository enrollmentRepository;
	private final UserReadingCompletionRepository completionRepository;
	private final UserReadingSegmentCompletionRepository segmentCompletionRepository;
	private final BibleService bibleService;
	private final BibleBookResolver bibleBookResolver;

	public ReadingProgressService(UserRepository userRepository, ReadingPlanRepository planRepository,
			ReadingPlanDayRepository planDayRepository, UserReadingEnrollmentRepository enrollmentRepository,
			UserReadingCompletionRepository completionRepository,
			UserReadingSegmentCompletionRepository segmentCompletionRepository, BibleService bibleService,
			BibleBookResolver bibleBookResolver) {
		this.userRepository = userRepository;
		this.planRepository = planRepository;
		this.planDayRepository = planDayRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.completionRepository = completionRepository;
		this.segmentCompletionRepository = segmentCompletionRepository;
		this.bibleService = bibleService;
		this.bibleBookResolver = bibleBookResolver;
	}

	private record SegmentSlot(long planDayId, int segmentIndex) {
	}

	@Transactional
	public EnrollmentSummaryResponse enroll(String username, EnrollReadingPlanRequest request) {
		User user = user(username);
		if (enrollmentRepository.findByUser_Id(user.getId()).isPresent()) {
			throw new EnrollmentAlreadyExistsException();
		}
		ReadingPlan plan = planRepository.findById(request.planId())
				.orElseThrow(() -> new ReadingPlanNotFoundException(request.planId()));
		long totalDays = planDayRepository.countDistinctDayNumbersByPlan_Id(plan.getId());
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
		long totalDaysLong = planDayRepository.countDistinctDayNumbersByPlan_Id(plan.getId());
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
		int totalDays = (int) planDayRepository.countDistinctDayNumbersByPlan_Id(plan.getId());
		LocalDate planStart = enrollment.getPlanStartDate();
		int scheduledDay = dayNumberForPlanDate(planStart, referenceDate);
		if (scheduledDay < 1 || scheduledDay > totalDays) {
			return new TodayBibleReadingResponse(referenceDate, null, null, v, false, List.of());
		}
		LocalDate scheduledDate = scheduledDateForDay(planStart, scheduledDay);
		List<ReadingPlanDay> dayRows = planDayRepository.findAllByPlan_IdAndDayNumberOrderBySegmentIndexAsc(plan.getId(),
				scheduledDay);
		boolean dayCompleted = completionRepository.existsByEnrollment_IdAndDayNumber(enrollment.getId(), scheduledDay);
		if (dayRows.isEmpty()) {
			return new TodayBibleReadingResponse(referenceDate, scheduledDay, scheduledDate, v, dayCompleted, List.of());
		}
		List<TodayBibleBlockResponse> blocks = buildTodayBibleBlocksForScheduledDay(enrollment.getId(), dayRows, v);
		return new TodayBibleReadingResponse(referenceDate, scheduledDay, scheduledDate, v, dayCompleted, blocks);
	}

	/**
	 * Várias linhas no mesmo dia (novo modelo) ou uma linha legada com {@code ;} no texto.
	 */
	private List<TodayBibleBlockResponse> buildTodayBibleBlocksForScheduledDay(long enrollmentId,
			List<ReadingPlanDay> rows, String versionId) {
		if (rows.size() == 1 && rows.get(0).getReadingText() != null && rows.get(0).getReadingText().contains(";")) {
			return buildTodayBibleBlocksLegacySemicolonRow(enrollmentId, rows.get(0), versionId);
		}
		List<TodayBibleBlockResponse> blocks = new ArrayList<>(rows.size());
		for (ReadingPlanDay row : rows) {
			blocks.add(buildTodayBibleBlockSingleRow(enrollmentId, row, versionId, 0));
		}
		return blocks;
	}

	private TodayBibleBlockResponse buildTodayBibleBlockSingleRow(long enrollmentId, ReadingPlanDay d, String versionId,
			int segmentIndex) {
		boolean blockDone = segmentCompletionRepository.existsByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(
				enrollmentId, d.getId(), segmentIndex);
		Integer bookNum = bibleBookResolver.resolveBookNumber(d.getBookName()).orElse(null);
		String abbrev = bookNum != null ? bibleBookResolver.abbrevForBook(bookNum).orElse(null) : null;
		List<BibleChapterResponse> chapters = new ArrayList<>();
		if (bookNum != null) {
			for (int ch = d.getStartChapter(); ch <= d.getEndChapter(); ch++) {
				chapters.add(bibleService.getChapter(versionId, bookNum, ch));
			}
		}
		return new TodayBibleBlockResponse(d.getId(), segmentIndex, d.getDayNumber(), d.getBookName(), bookNum, abbrev,
				d.getStartChapter(), d.getEndChapter(), d.getReadingText(), blockDone, List.copyOf(chapters));
	}

	private List<TodayBibleBlockResponse> buildTodayBibleBlocksLegacySemicolonRow(long enrollmentId, ReadingPlanDay d,
			String versionId) {
		List<ReadingPlanReadingSegments.Segment> segments = ReadingPlanReadingSegments.parse(d.getReadingText());
		if (segments.isEmpty()) {
			return List.of(buildTodayBibleBlockSingleRow(enrollmentId, d, versionId, 0));
		}
		List<TodayBibleBlockResponse> blocks = new ArrayList<>(segments.size());
		for (int i = 0; i < segments.size(); i++) {
			ReadingPlanReadingSegments.Segment seg = segments.get(i);
			boolean blockDone = segmentCompletionRepository.existsByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(
					enrollmentId, d.getId(), i);
			Integer bookNum = bibleBookResolver.resolveBookNumber(seg.bookName()).orElse(null);
			String abbrev = bookNum != null ? bibleBookResolver.abbrevForBook(bookNum).orElse(null) : null;
			List<BibleChapterResponse> chapters = new ArrayList<>();
			if (bookNum != null) {
				for (int ch = seg.startChapter(); ch <= seg.endChapter(); ch++) {
					chapters.add(bibleService.getChapter(versionId, bookNum, ch));
				}
			}
			blocks.add(new TodayBibleBlockResponse(d.getId(), i, d.getDayNumber(), seg.bookName(), bookNum, abbrev,
					seg.startChapter(), seg.endChapter(), seg.segmentText(), blockDone, List.copyOf(chapters)));
		}
		return blocks;
	}

	@Transactional(readOnly = true)
	public List<CalendarDayReadResponse> calendar(String username, LocalDate fromInclusive, LocalDate toInclusive) {
		if (fromInclusive == null || toInclusive == null) {
			throw new InvalidProgressDateException("Informe from e to (ISO-8601).");
		}
		validateInclusiveDateRange(fromInclusive, toInclusive);
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

	@Transactional(readOnly = true)
	public List<CalendarDayReadResponse> calendarYear(String username, int year) {
		if (year < 1970 || year > 2100) {
			throw new InvalidProgressDateException("Informe um ano entre 1970 e 2100.");
		}
		LocalDate fromInclusive = LocalDate.of(year, 1, 1);
		LocalDate toInclusive = LocalDate.of(year, 12, 31);
		return calendar(username, fromInclusive, toInclusive);
	}

	@Transactional(readOnly = true)
	public ReadingStatisticsResponse statistics(String username, LocalDate fromInclusive, LocalDate toInclusive) {
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		ReadingPlan plan = enrollment.getPlan();
		LocalDate planStart = enrollment.getPlanStartDate();
		long totalDaysLong = planDayRepository.countDistinctDayNumbersByPlan_Id(plan.getId());
		int totalDays = (int) totalDaysLong;
		long completedDays = completionRepository.countByEnrollment_Id(enrollment.getId());
		int annualPercent = totalDays == 0 ? 0 : (int) Math.round(100.0 * completedDays / totalDays);

		LocalDate today = LocalDate.now();
		LocalDate periodTo = toInclusive != null ? toInclusive : today;
		LocalDate yearStart = periodTo.with(TemporalAdjusters.firstDayOfYear());
		LocalDate defaultFrom = planStart.isAfter(yearStart) ? planStart : yearStart;
		LocalDate periodFrom = fromInclusive != null ? fromInclusive : defaultFrom;
		if (periodFrom.isBefore(planStart)) {
			periodFrom = planStart;
		}
		validateInclusiveDateRange(periodFrom, periodTo);

		Set<LocalDate> allReadDates = completionRepository.findAllDistinctReadDates(enrollment.getId());
		LocalDate streakRef = periodTo.isAfter(today) ? today : periodTo;
		int currentStreak = currentStreakDays(allReadDates, streakRef);
		int longestStreak = longestStreakDays(allReadDates);

		Set<LocalDate> readInPeriod = completionRepository.findDistinctReadDatesBetween(enrollment.getId(), periodFrom,
				periodTo);
		int daysReadInPeriod = readInPeriod.size();
		List<LocalDate> readDatesList = new ArrayList<>(new TreeSet<>(readInPeriod));

		LocalDate missedThrough = periodTo.isBefore(today) ? periodTo : today;
		int daysMissed = 0;
		if (!missedThrough.isBefore(periodFrom)) {
			for (LocalDate d = periodFrom; !d.isAfter(missedThrough); d = d.plusDays(1)) {
				int scheduled = dayNumberForPlanDate(planStart, d);
				if (scheduled < 1 || scheduled > totalDays) {
					continue;
				}
				if (!readInPeriod.contains(d)) {
					daysMissed++;
				}
			}
		}

		Integer nextMilestonePercent = null;
		Integer daysUntilNextMilestone = null;
		if (totalDays > 0 && completedDays < totalDays) {
			int[] milestones = { 25, 50, 75, 100 };
			for (int m : milestones) {
				int targetDays = (int) Math.ceil(totalDays * (m / 100.0));
				if (completedDays < targetDays) {
					nextMilestonePercent = m;
					daysUntilNextMilestone = Math.max(0, targetDays - (int) completedDays);
					break;
				}
			}
		}

		boolean hasMissedDaysInPeriod = daysMissed > 0;
		return new ReadingStatisticsResponse(plan.getId(), plan.getOriginalFilename(), planStart, periodFrom, periodTo,
				totalDays, (int) completedDays, daysReadInPeriod, daysMissed, hasMissedDaysInPeriod, currentStreak,
				longestStreak, annualPercent, Collections.unmodifiableList(readDatesList), nextMilestonePercent,
				daysUntilNextMilestone);
	}

	private static void validateInclusiveDateRange(LocalDate fromInclusive, LocalDate toInclusive) {
		if (toInclusive.isBefore(fromInclusive)) {
			throw new InvalidProgressDateException("O fim do intervalo não pode ser antes do início.");
		}
		long spanDays = ChronoUnit.DAYS.between(fromInclusive, toInclusive) + 1;
		if (spanDays > MAX_CALENDAR_RANGE_DAYS) {
			throw new InvalidProgressDateException(
					"Intervalo máximo de " + MAX_CALENDAR_RANGE_DAYS + " dias (inclusive). Reduza from/to.");
		}
	}

	@Transactional
	public void markDayRead(String username, MarkDayReadRequest request) {
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		long planId = enrollment.getPlan().getId();
		int totalDays = (int) planDayRepository.countDistinctDayNumbersByPlan_Id(planId);
		int dayNumber = request.dayNumber();
		if (dayNumber < 1 || dayNumber > totalDays) {
			throw new InvalidProgressDateException(
					"Dia fora do plano. Informe um número entre 1 e " + totalDays + ".");
		}
		LocalDate readDate = request.readDate() != null ? request.readDate() : LocalDate.now();
		if (request.readingPlanDayId() == null) {
			markAllSegmentsForDay(enrollment, planId, dayNumber, readDate);
		}
		else {
			long planDayId = request.readingPlanDayId();
			int segmentIndex = request.segmentIndex() != null ? request.segmentIndex() : 0;
			ReadingPlanDay row = planDayRepository.findById(planDayId)
					.orElseThrow(() -> new InvalidProgressDateException("Trecho de leitura não encontrado."));
			if (!row.getPlan().getId().equals(planId) || !row.getDayNumber().equals(dayNumber)) {
				throw new InvalidProgressDateException("O trecho não corresponde a este dia do seu plano.");
			}
			boolean slotOk = segmentSlotsForPlanDay(planId, dayNumber).stream()
					.anyMatch(s -> s.planDayId() == planDayId && s.segmentIndex() == segmentIndex);
			if (!slotOk) {
				throw new InvalidProgressDateException("Índice de trecho inválido para este dia.");
			}
			upsertSegmentCompletion(enrollment, row, segmentIndex, readDate);
			syncDayLevelCompletion(enrollment, dayNumber, readDate);
		}
	}

	@Transactional
	public void unmarkDayRead(String username, int dayNumber) {
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		long planId = enrollment.getPlan().getId();
		int totalDays = (int) planDayRepository.countDistinctDayNumbersByPlan_Id(planId);
		if (dayNumber < 1 || dayNumber > totalDays) {
			throw new InvalidProgressDateException(
					"Dia fora do plano. Informe um número entre 1 e " + totalDays + ".");
		}
		segmentCompletionRepository.deleteByEnrollmentAndPlanIdAndDayNumber(enrollment.getId(), planId, dayNumber);
		completionRepository.deleteByEnrollment_IdAndDayNumber(enrollment.getId(), dayNumber);
	}

	@Transactional
	public void unmarkSegmentRead(String username, long readingPlanDayId, int segmentIndex) {
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		ReadingPlanDay row = planDayRepository.findById(readingPlanDayId)
				.orElseThrow(() -> new InvalidProgressDateException("Trecho de leitura não encontrado."));
		if (!row.getPlan().getId().equals(enrollment.getPlan().getId())) {
			throw new InvalidProgressDateException("O trecho não pertence ao seu plano.");
		}
		segmentCompletionRepository.deleteByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(enrollment.getId(),
				readingPlanDayId, segmentIndex);
		syncDayLevelCompletion(enrollment, row.getDayNumber(), LocalDate.now());
	}

	private void markAllSegmentsForDay(UserReadingEnrollment enrollment, long planId, int dayNumber, LocalDate readDate) {
		for (SegmentSlot slot : segmentSlotsForPlanDay(planId, dayNumber)) {
			ReadingPlanDay row = planDayRepository.findById(slot.planDayId())
					.orElseThrow(() -> new IllegalStateException("reading_plan_day ausente: " + slot.planDayId()));
			upsertSegmentCompletion(enrollment, row, slot.segmentIndex(), readDate);
		}
		syncDayLevelCompletion(enrollment, dayNumber, readDate);
	}

	private List<SegmentSlot> segmentSlotsForPlanDay(long planId, int dayNumber) {
		List<ReadingPlanDay> rows = planDayRepository.findAllByPlan_IdAndDayNumberOrderBySegmentIndexAsc(planId,
				dayNumber);
		List<SegmentSlot> slots = new ArrayList<>();
		if (rows.size() == 1 && rows.get(0).getReadingText() != null && rows.get(0).getReadingText().contains(";")) {
			ReadingPlanDay d = rows.get(0);
			List<ReadingPlanReadingSegments.Segment> segments = ReadingPlanReadingSegments.parse(d.getReadingText());
			if (segments.isEmpty()) {
				slots.add(new SegmentSlot(d.getId(), 0));
			}
			else {
				for (int i = 0; i < segments.size(); i++) {
					slots.add(new SegmentSlot(d.getId(), i));
				}
			}
		}
		else {
			for (ReadingPlanDay d : rows) {
				slots.add(new SegmentSlot(d.getId(), 0));
			}
		}
		return slots;
	}

	private void upsertSegmentCompletion(UserReadingEnrollment enrollment, ReadingPlanDay planDay, int segmentIndex,
			LocalDate readDate) {
		UserReadingSegmentCompletion row = segmentCompletionRepository
				.findByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(enrollment.getId(), planDay.getId(), segmentIndex)
				.orElseGet(UserReadingSegmentCompletion::new);
		if (row.getId() == null) {
			row.setEnrollment(enrollment);
			row.setReadingPlanDay(planDay);
			row.setSegmentIndex(segmentIndex);
		}
		row.setReadDate(readDate);
		segmentCompletionRepository.save(row);
	}

	/**
	 * Mantém {@link UserReadingCompletion} só quando todos os trechos do dia estão marcados (calendário / progresso).
	 */
	private void syncDayLevelCompletion(UserReadingEnrollment enrollment, int dayNumber, LocalDate readDateForCompletion) {
		long planId = enrollment.getPlan().getId();
		List<SegmentSlot> slots = segmentSlotsForPlanDay(planId, dayNumber);
		for (SegmentSlot slot : slots) {
			if (!segmentCompletionRepository.existsByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(enrollment.getId(),
					slot.planDayId(), slot.segmentIndex())) {
				completionRepository.deleteByEnrollment_IdAndDayNumber(enrollment.getId(), dayNumber);
				return;
			}
		}
		UserReadingCompletion row = completionRepository.findByEnrollment_IdAndDayNumber(enrollment.getId(), dayNumber)
				.orElseGet(UserReadingCompletion::new);
		if (row.getId() == null) {
			row.setEnrollment(enrollment);
			row.setDayNumber(dayNumber);
		}
		row.setReadDate(readDateForCompletion);
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
		validateInclusiveDateRange(from, to);
		catchUpRangeForEnrollment(enrollment, from, to);
	}

	@Transactional
	public void catchUpByDateRanges(String username, CatchUpBatchDateRangesRequest request) {
		UserReadingEnrollment enrollment = enrollmentRepository.findByUser_Id(user(username).getId())
				.orElseThrow(NoActiveEnrollmentException::new);
		for (CatchUpDateRangeRequest range : request.ranges()) {
			LocalDate from = range.fromInclusive();
			LocalDate to = range.toInclusive();
			if (to.isBefore(from)) {
				throw new InvalidProgressDateException("O fim do intervalo não pode ser antes do início.");
			}
			validateInclusiveDateRange(from, to);
			catchUpRangeForEnrollment(enrollment, from, to);
		}
	}

	private void catchUpRangeForEnrollment(UserReadingEnrollment enrollment, LocalDate fromInclusive,
			LocalDate toInclusive) {
		LocalDate planStart = enrollment.getPlanStartDate();
		long planId = enrollment.getPlan().getId();
		long totalDaysLong = planDayRepository.countDistinctDayNumbersByPlan_Id(planId);
		int totalDays = (int) totalDaysLong;
		for (LocalDate d = fromInclusive; !d.isAfter(toInclusive); d = d.plusDays(1)) {
			int dayNumber = dayNumberForPlanDate(planStart, d);
			if (dayNumber < 1 || dayNumber > totalDays) {
				continue;
			}
			markAllSegmentsForDay(enrollment, planId, dayNumber, d);
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
		long planId = enrollment.getPlan().getId();
		int bound = lastDay;
		IntStream.rangeClosed(1, bound).forEach(dayNumber -> {
			LocalDate scheduled = scheduledDateForDay(planStart, dayNumber);
			markAllSegmentsForDay(enrollment, planId, dayNumber, scheduled);
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
		List<ReadingPlanDay> dayRows = planDayRepository.findAllByPlan_IdAndDayNumberOrderBySegmentIndexAsc(planId,
				scheduledDay);
		List<TodayReadingBlockResponse> blocks = buildTodayReadingSectionBlocksForScheduledDay(enrollment.getId(),
				dayRows);
		return new TodayReadingSectionResponse(ref, scheduledDay, scheduledDate, blocks);
	}

	private List<TodayReadingBlockResponse> buildTodayReadingSectionBlocksForScheduledDay(long enrollmentId,
			List<ReadingPlanDay> rows) {
		if (rows.isEmpty()) {
			return List.of();
		}
		if (rows.size() == 1 && rows.get(0).getReadingText() != null && rows.get(0).getReadingText().contains(";")) {
			ReadingPlanDay d = rows.get(0);
			List<ReadingPlanReadingSegments.Segment> segments = ReadingPlanReadingSegments.parse(d.getReadingText());
			if (segments.isEmpty()) {
				segments = List.of(new ReadingPlanReadingSegments.Segment(d.getBookName(), d.getStartChapter(),
						d.getEndChapter(), d.getReadingText()));
			}
			List<TodayReadingBlockResponse> blocks = new ArrayList<>(segments.size());
			for (int i = 0; i < segments.size(); i++) {
				ReadingPlanReadingSegments.Segment seg = segments.get(i);
				boolean blockDone = segmentCompletionRepository.existsByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(
						enrollmentId, d.getId(), i);
				Integer bookNum = bibleBookResolver.resolveBookNumber(seg.bookName()).orElse(null);
				String abbrev = bookNum != null ? bibleBookResolver.abbrevForBook(bookNum).orElse(null) : null;
				blocks.add(new TodayReadingBlockResponse(d.getId(), i, d.getDayNumber(), seg.bookName(), bookNum, abbrev,
						seg.startChapter(), seg.endChapter(), seg.segmentText(), blockDone));
			}
			return blocks;
		}
		List<TodayReadingBlockResponse> blocks = new ArrayList<>(rows.size());
		for (ReadingPlanDay d : rows) {
			boolean blockDone = segmentCompletionRepository.existsByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(
					enrollmentId, d.getId(), 0);
			Integer bookNum = bibleBookResolver.resolveBookNumber(d.getBookName()).orElse(null);
			String abbrev = bookNum != null ? bibleBookResolver.abbrevForBook(bookNum).orElse(null) : null;
			blocks.add(new TodayReadingBlockResponse(d.getId(), 0, d.getDayNumber(), d.getBookName(), bookNum, abbrev,
					d.getStartChapter(), d.getEndChapter(), d.getReadingText(), blockDone));
		}
		return blocks;
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

	static int longestStreakDays(Set<LocalDate> readDates) {
		if (readDates == null || readDates.isEmpty()) {
			return 0;
		}
		List<LocalDate> sorted = new ArrayList<>(readDates);
		Collections.sort(sorted);
		int longest = 1;
		int run = 1;
		for (int i = 1; i < sorted.size(); i++) {
			LocalDate prev = sorted.get(i - 1);
			LocalDate cur = sorted.get(i);
			if (cur.equals(prev.plusDays(1))) {
				run++;
				if (run > longest) {
					longest = run;
				}
			}
			else if (!cur.equals(prev)) {
				run = 1;
			}
		}
		return longest;
	}
}
