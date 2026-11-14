package com.overex.backend.event;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Event> findByCreatedByIdAndStatus(UUID userId, Status status, Pageable pageable);

    Page<Event> findByCreatedById(UUID id, Pageable pageable);

    long countByCreatedByIdAndStatus(UUID userId, Status status);

    @Query("""
            SELECT e FROM Event e
            WHERE e.createdBy.id = :userId
              AND (:status IS NULL OR e.status = :status)
              AND (:categoryId IS NULL OR e.category.id = :categoryId)
            """)
    Page<Event> findMyEvents(@Param("userId") UUID userId,
            @Param("status") Status status,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

}
