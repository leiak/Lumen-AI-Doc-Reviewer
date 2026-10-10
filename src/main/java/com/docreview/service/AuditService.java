package com.docreview.service;

import com.docreview.persistence.entity.AuditLogEntity;
import com.docreview.persistence.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String sessionId, String action, String detail) {
        AuditLogEntity log = new AuditLogEntity();
        log.setSessionId(sessionId);
        log.setAction(action);
        log.setDetail(detail);
        repository.save(log);
    }

    public List<AuditLogEntity> findBySession(String sessionId) {
        return repository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }
}
