package com.daily.bread.readingplan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.daily.bread.readingplan.model.ReadingPlanDay;

public interface ReadingPlanDayRepository extends JpaRepository<ReadingPlanDay, Long> {

	long countByPlan_Id(Long planId);

	Optional<ReadingPlanDay> findByPlan_IdAndDayNumber(Long planId, int dayNumber);
}
