package com.daily.bread.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.daily.bread.auth.model.User;
import com.daily.bread.auth.repository.UserRepository;

@Component
@Order(0)
@ConditionalOnProperty(name = "bread.seed-test-user", havingValue = "true")
public class TestUserSeeder implements ApplicationRunner {

	static final String USERNAME = "teste";
	static final String EMAIL = "teste@dailybread.local";
	static final String PASSWORD = "123456789";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public TestUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.existsByUsernameIgnoreCase(USERNAME)) {
			return;
		}
		User user = new User();
		user.setName("Usuário Teste");
		user.setEmail(EMAIL);
		user.setUsername(USERNAME);
		user.setPasswordHash(passwordEncoder.encode(PASSWORD));
		userRepository.save(user);
	}
}
