package com.overex.backend.event.dto.responses;

import com.overex.backend.event.Status;

import java.util.Map;

public record EventDetailsResponseDto(EventResponseDto event, Map<Status, StatusOption> statusOptions) {
    
}
