package com.docreview.persistence.repository;

import com.docreview.persistence.entity.DocumentSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSessionRepository extends JpaRepository<DocumentSessionEntity, String> {
    boolean existsByParentSessionId(String parentSessionId);
}
