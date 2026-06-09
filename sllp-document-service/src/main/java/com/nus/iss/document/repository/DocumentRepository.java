package com.nus.iss.document.repository;

import com.nus.iss.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Document entity.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    /**
     * Find document by ID and verify ownership (user_id matches).
     *
     * @param id document ID
     * @param userId user ID
     * @return optional containing document if found and owned by user
     */
    Optional<Document> findByIdAndUserId(String id, String userId);

    /**
     * Find all documents for a user (paginated).
     *
     * @param userId user ID
     * @param pageable pagination parameters
     * @return page of documents owned by user
     */
    Page<Document> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Delete document by ID and verify ownership.
     *
     * @param id document ID
     * @param userId user ID
     * @return number of documents deleted
     */
    long deleteByIdAndUserId(String id, String userId);
}
