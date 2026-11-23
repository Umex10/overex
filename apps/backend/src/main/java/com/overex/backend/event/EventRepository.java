package com.overex.backend.event;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {

  Page<Event> findByStatusAndStartsAtAfter(Status status, Instant startsAfter, Pageable pageable);

  Page<Event> findByCategoryIdAndStatusAndStartsAtAfter(UUID categoryId, Status status, Instant startsAfter,
      Pageable pageable);

  Page<Event> findByCreatedByIdAndStatus(UUID userId, Status status, Pageable pageable);

  Page<Event> findByCreatedById(UUID id, Pageable pageable);

  @Query("""
      SELECT e FROM Event e
      WHERE e.createdBy.id = :userId
        AND (:status IS NULL OR e.status = :status)
        AND (:categoryId IS NULL OR e.category.id = :categoryId)
        AND (CAST(:startsAfter AS Instant) IS NULL OR e.startsAt > :startsAfter)
        AND (CAST(:startsBeforeOrAt AS Instant) IS NULL OR e.startsAt <= :startsBeforeOrAt)
      """)
  Page<Event> findMyEvents(@Param("userId") UUID userId,
      @Param("status") Status status,
      @Param("categoryId") UUID categoryId,
      @Param("startsAfter") Instant startsAfter,
      @Param("startsBeforeOrAt") Instant startsBeforeOrAt,
      Pageable pageable);

}
