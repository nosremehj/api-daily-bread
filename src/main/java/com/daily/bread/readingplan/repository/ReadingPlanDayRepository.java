package com.daily.bread.readingplan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.daily.bread.readingplan.model.ReadingPlanDay;

public interface ReadingPlanDayRepository extends JpaRepository<ReadingPlanDay, Long> {

	long countByPlan_Id(Long planId);
}
