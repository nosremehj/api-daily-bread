package com.daily.bread.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.daily.bread.auth.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsernameIgnoreCase(String username);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);
}
