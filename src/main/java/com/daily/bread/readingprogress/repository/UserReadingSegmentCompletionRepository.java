package com.daily.bread.readingprogress.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.daily.bread.readingprogress.model.UserReadingSegmentCompletion;

public interface UserReadingSegmentCompletionRepository extends JpaRepository<UserReadingSegmentCompletion, Long> {

	boolean existsByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(long enrollmentId, long readingPlanDayId,
			int segmentIndex);

	Optional<UserReadingSegmentCompletion> findByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(long enrollmentId,
			long readingPlanDayId, int segmentIndex);

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM UserReadingSegmentCompletion s WHERE s.enrollment.id = :enrollmentId AND s.readingPlanDay.plan.id = :planId AND s.readingPlanDay.dayNumber = :dayNumber")
	int deleteByEnrollmentAndPlanIdAndDayNumber(@Param("enrollmentId") long enrollmentId, @Param("planId") long planId,
			@Param("dayNumber") int dayNumber);

	@Modifying(clearAutomatically = true)
	void deleteByEnrollment_IdAndReadingPlanDay_IdAndSegmentIndex(long enrollmentId, long readingPlanDayId,
			int segmentIndex);
}
