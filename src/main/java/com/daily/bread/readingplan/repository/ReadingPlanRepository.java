package com.daily.bread.readingplan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.daily.bread.readingplan.model.ReadingPlan;

public interface ReadingPlanRepository extends JpaRepository<ReadingPlan, Long> {

	@Query("SELECT DISTINCT p FROM ReadingPlan p LEFT JOIN FETCH p.days WHERE p.id = :id")
	Optional<ReadingPlan> findByIdWithDays(@Param("id") Long id);

	Optional<ReadingPlan> findFirstByPdfSha256(String pdfSha256);

	/** Planos criados antes do hash: compara o BLOB para evitar reimportar o mesmo PDF. */
	@Query(value = "SELECT id FROM reading_plans WHERE pdf_sha256 IS NULL AND pdf_content = ?1 LIMIT 1", nativeQuery = true)
	Optional<Long> findLegacyIdWithSamePdfContent(byte[] pdfContent);
}
