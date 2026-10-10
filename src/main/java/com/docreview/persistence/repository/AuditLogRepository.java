package com.docreview.persistence.repository;

import com.docreview.persistence.entity.AuditLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
