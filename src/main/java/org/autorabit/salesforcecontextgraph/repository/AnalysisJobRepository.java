package org.autorabit.salesforcecontextgraph.repository;

import org.autorabit.salesforcecontextgraph.domain.entity.AnalysisJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJobEntity, Long> {
}
