package com.daily.bread.favorites.controllers;

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

import com.daily.bread.favorites.request.AddVerseFavoriteRequest;
import com.daily.bread.favorites.response.VerseFavoriteResponse;
import com.daily.bread.favorites.services.VerseFavoriteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/verse-favorites")
public class VerseFavoriteController {

	private final VerseFavoriteService verseFavoriteService;

	public VerseFavoriteController(VerseFavoriteService verseFavoriteService) {
		this.verseFavoriteService = verseFavoriteService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VerseFavoriteResponse add(Authentication authentication, @Valid @RequestBody AddVerseFavoriteRequest request) {
		return verseFavoriteService.add(authentication.getName(), request);
	}

	@GetMapping
	public List<VerseFavoriteResponse> listByReadingDate(Authentication authentication,
			@RequestParam LocalDate readingDate) {
		return verseFavoriteService.listByReadingDate(authentication.getName(), readingDate);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(Authentication authentication, @PathVariable Long id) {
		verseFavoriteService.delete(authentication.getName(), id);
	}
}
