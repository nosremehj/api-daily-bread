package com.daily.bread.readingplan.services;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

	private final ReadingPlanRepository planRepository;
	private final ReadingPlanDayRepository dayRepository;
	private final ReadingPlanPdfParser pdfParser;

	public ReadingPlanService(ReadingPlanRepository planRepository, ReadingPlanDayRepository dayRepository,
			ReadingPlanPdfParser pdfParser) {
		this.planRepository = planRepository;
		this.dayRepository = dayRepository;
		this.pdfParser = pdfParser;
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
		List<ParsedReadingDay> parsed = pdfParser.parse(bytes);

		ReadingPlan plan = new ReadingPlan();
		plan.setOriginalFilename(name);
		plan.setPdfContent(bytes);
		plan.setImportedAt(Instant.now());
		for (ParsedReadingDay p : parsed) {
			ReadingPlanDay day = new ReadingPlanDay();
			day.setDayNumber(p.dayNumber());
			day.setBookName(p.bookName());
			day.setStartChapter(p.startChapter());
			day.setEndChapter(p.endChapter());
			day.setReadingText(p.readingText());
			day.setCompleted(false);
			plan.addDay(day);
		}
		ReadingPlan saved = planRepository.save(plan);
		return new ReadingPlanImportResponse(saved.getId(), saved.getOriginalFilename(), saved.getImportedAt(),
				parsed.size());
	}

	@Transactional(readOnly = true)
	public List<ReadingPlanSummaryResponse> listSummaries() {
		return planRepository.findAll(Sort.by(Sort.Direction.DESC, "importedAt")).stream()
				.map(p -> new ReadingPlanSummaryResponse(p.getId(), p.getOriginalFilename(), p.getImportedAt(),
						dayRepository.countByPlan_Id(p.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public ReadingPlanResponse getById(Long id) {
		ReadingPlan plan = planRepository.findByIdWithDays(id).orElseThrow(() -> new ReadingPlanNotFoundException(id));
		List<ReadingPlanDayResponse> days = plan.getDays().stream()
				.map(d -> new ReadingPlanDayResponse(d.getId(), d.getDayNumber(), d.getBookName(), d.getStartChapter(),
						d.getEndChapter(), d.getReadingText(), d.isCompleted()))
				.toList();
		return new ReadingPlanResponse(plan.getId(), plan.getOriginalFilename(), plan.getImportedAt(), days);
	}
}
