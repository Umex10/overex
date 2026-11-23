package com.overex.backend.event.dto.responses;

import java.util.UUID;

import lombok.Builder;

@Builder 
public record CategoryResponseDto(UUID id, String title) {
    
}
