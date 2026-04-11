package com.daily.bread.auth.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.daily.bread.auth.request.AuthRequests;
import com.daily.bread.auth.request.LoginRequest;
import com.daily.bread.auth.request.RefreshRequest;
import com.daily.bread.auth.request.RegisterRequest;
import com.daily.bread.auth.response.AuthResponse;
import com.daily.bread.auth.response.ProfileUpdateResponse;
import com.daily.bread.auth.response.UserResponse;
import com.daily.bread.auth.services.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request);
	}

	@GetMapping("/me")
	public UserResponse me(Authentication authentication) {
		return authService.me(authentication.getName());
	}

	@PatchMapping("/me")
	public ProfileUpdateResponse updateProfile(
			Authentication authentication, @Valid @RequestBody AuthRequests.UpdateProfileRequest request) {
		return authService.updateProfile(authentication.getName(), request);
	}

	@PostMapping("/change-password")
	public AuthResponse changePassword(
			Authentication authentication, @Valid @RequestBody AuthRequests.ChangePasswordRequest request) {
		return authService.changePassword(authentication.getName(), request);
	}
}
