package com.daily.bread.readingplan.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.daily.bread.readingplan.response.ReadingPlanImportResponse;
import com.daily.bread.readingplan.response.ReadingPlanResponse;
import com.daily.bread.readingplan.response.ReadingPlanSummaryResponse;
import com.daily.bread.readingplan.services.ReadingPlanService;

@RestController
@RequestMapping("/api/v1/reading-plans")
public class ReadingPlanController {

	private final ReadingPlanService readingPlanService;

	public ReadingPlanController(ReadingPlanService readingPlanService) {
		this.readingPlanService = readingPlanService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ReadingPlanImportResponse importPdf(@RequestPart("file") MultipartFile file) {
		return readingPlanService.importFromPdf(file);
	}

	@GetMapping
	public List<ReadingPlanSummaryResponse> list() {
		return readingPlanService.listSummaries();
	}

	@GetMapping("/{id}")
	public ReadingPlanResponse get(@PathVariable Long id) {
		return readingPlanService.getById(id);
	}
}
