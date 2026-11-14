package com.overex.backend.event.dto.responses;

import lombok.Builder;

@Builder 
public record CountsOfEventsDto(long countOfAllEvents, long countOfCanceledEvents, long countOfDraftEvents, long countOfPublishedEvents) {

}
