package org.autorabit.salesforcecontextgraph.repository;

import java.util.Optional;
import org.autorabit.salesforcecontextgraph.domain.entity.AnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResultEntity, Long> {
    Optional<AnalysisResultEntity> findByJobId(Long jobId);
}
