package com.daily.bread.readingprogress.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.daily.bread.readingprogress.model.UserReadingCompletion;

public interface UserReadingCompletionRepository extends JpaRepository<UserReadingCompletion, Long> {

	long countByEnrollment_Id(Long enrollmentId);

	@Query("SELECT DISTINCT c.readDate FROM UserReadingCompletion c WHERE c.enrollment.id = :enrollmentId AND c.readDate BETWEEN :fromInclusive AND :toInclusive")
	Set<LocalDate> findDistinctReadDatesBetween(@Param("enrollmentId") Long enrollmentId,
			@Param("fromInclusive") LocalDate fromInclusive, @Param("toInclusive") LocalDate toInclusive);

	@Query("SELECT DISTINCT c.readDate FROM UserReadingCompletion c WHERE c.enrollment.id = :enrollmentId")
	Set<LocalDate> findAllDistinctReadDates(@Param("enrollmentId") Long enrollmentId);

	void deleteByEnrollment_Id(Long enrollmentId);

	List<UserReadingCompletion> findAllByEnrollment_Id(Long enrollmentId);

	boolean existsByEnrollment_IdAndDayNumber(Long enrollmentId, int dayNumber);

	Optional<UserReadingCompletion> findByEnrollment_IdAndDayNumber(Long enrollmentId, int dayNumber);

	void deleteByEnrollment_IdAndDayNumber(Long enrollmentId, int dayNumber);
}
