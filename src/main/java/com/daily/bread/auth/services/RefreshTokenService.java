package com.daily.bread.auth.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daily.bread.auth.model.RefreshToken;
import com.daily.bread.auth.model.User;
import com.daily.bread.auth.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final long refreshExpirationMs;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			@Value("${bread.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshExpirationMs = refreshExpirationMs;
	}

	@Transactional
	public String createForUser(User user) {
		byte[] raw = new byte[32];
		secureRandom.nextBytes(raw);
		String plain = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		String hash = TokenHasher.sha256Hex(plain);

		RefreshToken entity = new RefreshToken();
		entity.setUser(user);
		entity.setTokenHash(hash);
		entity.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
		refreshTokenRepository.save(entity);
		return plain;
	}
}
