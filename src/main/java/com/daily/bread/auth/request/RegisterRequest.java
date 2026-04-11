package com.daily.bread.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 256) String name,
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(min = 3, max = 64) String username,
		@NotBlank @Size(min = 8, max = 128) String password) {
}
