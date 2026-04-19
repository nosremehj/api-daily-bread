package com.daily.bread.readingplan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.daily.bread.readingplan.model.ReadingPlanDay;

public interface ReadingPlanDayRepository extends JpaRepository<ReadingPlanDay, Long> {

	long countByPlan_Id(Long planId);

	@Query("SELECT COUNT(DISTINCT d.dayNumber) FROM ReadingPlanDay d WHERE d.plan.id = :planId")
	long countDistinctDayNumbersByPlan_Id(@Param("planId") Long planId);

	List<ReadingPlanDay> findAllByPlan_IdAndDayNumberOrderBySegmentIndexAsc(Long planId, int dayNumber);
}
