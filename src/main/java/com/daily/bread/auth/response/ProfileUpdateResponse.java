package com.daily.bread.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ProfileUpdateResponse(
		UserResponse user,
		@JsonInclude(JsonInclude.Include.NON_NULL) AuthResponse newSession) {
}
