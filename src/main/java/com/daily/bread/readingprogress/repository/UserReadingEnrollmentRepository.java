package com.daily.bread.readingprogress.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.daily.bread.readingprogress.model.UserReadingEnrollment;

public interface UserReadingEnrollmentRepository extends JpaRepository<UserReadingEnrollment, Long> {

	Optional<UserReadingEnrollment> findByUser_Id(Long userId);
}
