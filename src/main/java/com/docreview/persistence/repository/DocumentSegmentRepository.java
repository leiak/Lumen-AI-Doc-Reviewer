package com.docreview.persistence.repository;

import com.docreview.persistence.entity.DocumentSegmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSegmentRepository extends JpaRepository<DocumentSegmentEntity, Long> {
    List<DocumentSegmentEntity> findBySessionIdOrderByOrderIndexAsc(String sessionId);
    void deleteBySessionId(String sessionId);
}
