package com.daily.bread.readingplan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.daily.bread.readingplan.model.ReadingPlan;

public interface ReadingPlanRepository extends JpaRepository<ReadingPlan, Long> {

	@Query("SELECT DISTINCT p FROM ReadingPlan p LEFT JOIN FETCH p.days WHERE p.id = :id")
	Optional<ReadingPlan> findByIdWithDays(@Param("id") Long id);
}
