package org.autorabit.salesforcecontextgraph.service;

import org.autorabit.salesforcecontextgraph.domain.entity.AnalysisJobEntity;
import org.autorabit.salesforcecontextgraph.domain.entity.AnalysisResultEntity;
import org.autorabit.salesforcecontextgraph.repository.AnalysisJobRepository;
import org.autorabit.salesforcecontextgraph.repository.AnalysisResultRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditAgent {

    private final AnalysisJobRepository jobRepository;
    private final AnalysisResultRepository resultRepository;

    public AuditAgent(AnalysisJobRepository jobRepository, AnalysisResultRepository resultRepository) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
    }

    public AnalysisJobEntity saveJob(AnalysisJobEntity job) {
        return jobRepository.save(job);
    }

    public AnalysisResultEntity saveResult(AnalysisResultEntity result) {
        return resultRepository.save(result);
    }
}
