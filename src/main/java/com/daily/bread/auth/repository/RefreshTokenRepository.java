package com.daily.bread.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.daily.bread.auth.model.RefreshToken;
import com.daily.bread.auth.model.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	void deleteAllByUser(User user);
}
