package com.daily.bread.auth.services;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.daily.bread.auth.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(
			@Value("${bread.jwt.secret}") String secret,
			@Value("${bread.jwt.expiration-ms}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("uid", user.getId())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.signWith(key)
				.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isValid(String token) {
		try {
			parseClaims(token);
			return true;
		}
		catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
