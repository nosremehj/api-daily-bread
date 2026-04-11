package com.daily.bread.auth.response;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, UserResponse user) {
}
