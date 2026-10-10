package com.docreview.persistence.repository;

import com.docreview.persistence.entity.DocFindingEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocFindingRepository extends JpaRepository<DocFindingEntity, Long> {
    List<DocFindingEntity> findBySessionIdOrderBySeverityAscIdAsc(String sessionId);
    List<DocFindingEntity> findBySessionIdAndUserActionIsNull(String sessionId);
    void deleteBySessionId(String sessionId);
}
