package com.overex.backend.event.dto.responses;

import java.time.Instant;

import java.util.UUID;

import com.overex.backend.event.Status;

import lombok.Builder;

@Builder
public record EventResponseDto(UUID id, Long version, CategoryResponseDto category,
        String title, String description, String venue, String city,
        Instant startsAt, Instant saleStartsAt, String imageUrl,
        Status status,
        Instant createdAt, Instant updatedAt) {

}
