package com.daily.bread.readingplan.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.daily.bread.bible.services.BibleBookResolver;
import com.daily.bread.bible.services.BibleService;
import com.daily.bread.readingplan.exceptions.ReadingPlanDuplicateException;
import com.daily.bread.readingplan.exceptions.ReadingPlanNotFoundException;
import com.daily.bread.readingplan.exceptions.ReadingPlanParseException;
import com.daily.bread.readingplan.model.ReadingPlan;
import com.daily.bread.readingplan.model.ReadingPlanDay;
import com.daily.bread.readingplan.repository.ReadingPlanDayRepository;
import com.daily.bread.readingplan.repository.ReadingPlanRepository;
import com.daily.bread.readingplan.response.ReadingPlanDayResponse;
import com.daily.bread.readingplan.response.ReadingPlanImportResponse;
import com.daily.bread.readingplan.response.ReadingPlanResponse;
import com.daily.bread.readingplan.response.ReadingPlanSummaryResponse;

@Service
public class ReadingPlanService {

	private static final String PLAN_CHAPTER_REFERENCE_VERSION = "nvi";

	private final ReadingPlanRepository planRepository;
	private final ReadingPlanDayRepository dayRepository;
	private final ReadingPlanPdfParser pdfParser;
	private final BibleBookResolver bibleBookResolver;
	private final BibleService bibleService;

	public ReadingPlanService(ReadingPlanRepository planRepository, ReadingPlanDayRepository dayRepository,
			ReadingPlanPdfParser pdfParser, BibleBookResolver bibleBookResolver, BibleService bibleService) {
		this.planRepository = planRepository;
		this.dayRepository = dayRepository;
		this.pdfParser = pdfParser;
		this.bibleBookResolver = bibleBookResolver;
		this.bibleService = bibleService;
	}

	@Transactional
	public ReadingPlanImportResponse importFromPdf(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ReadingPlanParseException("Envie um arquivo PDF no campo \"file\".");
		}
		String name = file.getOriginalFilename();
		if (name == null || !name.toLowerCase().endsWith(".pdf")) {
			throw new ReadingPlanParseException("O arquivo deve ser um PDF (.pdf).");
		}
		byte[] bytes;
		try {
			bytes = file.getBytes();
		}
		catch (java.io.IOException e) {
			throw new ReadingPlanParseException("Não foi possível ler o arquivo enviado.", e);
		}
		String pdfSha256 = sha256Hex(bytes);
		planRepository.findFirstByPdfSha256(pdfSha256).ifPresent(p -> {
			throw new ReadingPlanDuplicateException(p.getId());
		});
		planRepository.findLegacyIdWithSamePdfContent(bytes).ifPresent(id -> {
			throw new ReadingPlanDuplicateException(id);
		});

		List<ParsedPlanRow> parsed = pdfParser.parse(bytes);

		ReadingPlan plan = new ReadingPlan();
		plan.setOriginalFilename(name);
		plan.setPdfContent(bytes);
		plan.setPdfSha256(pdfSha256);
		plan.setImportedAt(Instant.now());
		for (ParsedPlanRow p : parsed) {
			ReadingPlanDay day = new ReadingPlanDay();
			day.setDayNumber(p.dayNumber());
			day.setSegmentIndex(p.segmentIndex());
			day.setBookName(p.bookName());
			Integer bookNum = bibleBookResolver.resolveBookNumber(p.bookName()).orElse(null);
			int start = p.startChapter() != null ? p.startChapter() : 1;
			int end;
			if (p.endChapter() != null) {
				end = p.endChapter();
			}
			else if (bookNum != null) {
				end = bibleService.getChapterCount(PLAN_CHAPTER_REFERENCE_VERSION, bookNum);
			}
			else {
				end = 1;
			}
			day.setStartChapter(start);
			day.setEndChapter(end);
			day.setReadingText(p.bookName() + " " + start + "-" + end);
			day.setCompleted(false);
			plan.addDay(day);
		}
		ReadingPlan saved = planRepository.save(plan);
		int calendarDays = (int) parsed.stream().mapToInt(ParsedPlanRow::dayNumber).distinct().count();
		return new ReadingPlanImportResponse(saved.getId(), saved.getOriginalFilename(), saved.getImportedAt(),
				calendarDays, parsed.size());
	}

	@Transactional(readOnly = true)
	public List<ReadingPlanSummaryResponse> listSummaries() {
		return planRepository.findAll(Sort.by(Sort.Direction.DESC, "importedAt")).stream()
				.map(p -> new ReadingPlanSummaryResponse(p.getId(), p.getOriginalFilename(), p.getImportedAt(),
						dayRepository.countDistinctDayNumbersByPlan_Id(p.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public ReadingPlanResponse getById(Long id) {
		ReadingPlan plan = planRepository.findByIdWithDays(id).orElseThrow(() -> new ReadingPlanNotFoundException(id));
		List<ReadingPlanDayResponse> days = plan.getDays().stream()
				.map(d -> new ReadingPlanDayResponse(d.getId(), d.getDayNumber(), d.getSegmentIndex(), d.getBookName(),
						d.getStartChapter(), d.getEndChapter(), d.getReadingText(), d.isCompleted()))
				.toList();
		return new ReadingPlanResponse(plan.getId(), plan.getOriginalFilename(), plan.getImportedAt(), days);
	}

	private static String sha256Hex(byte[] data) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 não disponível", e);
		}
	}
}
