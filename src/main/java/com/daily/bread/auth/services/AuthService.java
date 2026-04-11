package com.daily.bread.auth.services;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daily.bread.auth.exceptions.EmailAlreadyExistsException;
import com.daily.bread.auth.exceptions.InvalidCredentialsException;
import com.daily.bread.auth.exceptions.InvalidRefreshTokenException;
import com.daily.bread.auth.exceptions.UsernameAlreadyExistsException;
import com.daily.bread.auth.model.RefreshToken;
import com.daily.bread.auth.model.User;
import com.daily.bread.auth.repository.RefreshTokenRepository;
import com.daily.bread.auth.repository.UserRepository;
import com.daily.bread.auth.request.LoginRequest;
import com.daily.bread.auth.request.RefreshRequest;
import com.daily.bread.auth.request.RegisterRequest;
import com.daily.bread.auth.response.AuthResponse;
import com.daily.bread.auth.response.UserResponse;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			RefreshTokenService refreshTokenService) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		String username = request.username().trim();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new EmailAlreadyExistsException("E-mail já cadastrado.");
		}
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new UsernameAlreadyExistsException("Nome de usuário já em uso.");
		}
		User user = new User();
		user.setName(request.name().trim());
		user.setEmail(email);
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user = userRepository.save(user);
		return buildAuthResponse(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByUsernameIgnoreCase(request.username().trim())
				.orElseThrow(() -> new InvalidCredentialsException("Usuário ou senha inválidos."));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException("Usuário ou senha inválidos.");
		}
		return buildAuthResponse(user);
	}

	@Transactional
	public AuthResponse refresh(RefreshRequest request) {
		String hash = TokenHasher.sha256Hex(request.refreshToken().trim());
		RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new InvalidRefreshTokenException("Refresh token inválido ou expirado."));
		if (stored.getExpiresAt().isBefore(Instant.now())) {
			refreshTokenRepository.delete(stored);
			throw new InvalidRefreshTokenException("Refresh token inválido ou expirado.");
		}
		User user = stored.getUser();
		refreshTokenRepository.delete(stored);
		return buildAuthResponse(user);
	}

	public UserResponse me(String username) {
		User user = userRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new InvalidCredentialsException("Sessão inválida."));
		return toUserResponse(user);
	}

	private AuthResponse buildAuthResponse(User user) {
		String accessToken = jwtService.generateToken(user);
		String refreshToken = refreshTokenService.createForUser(user);
		return new AuthResponse(accessToken, refreshToken, "Bearer", toUserResponse(user));
	}

	private static UserResponse toUserResponse(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getUsername());
	}
}
