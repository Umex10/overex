package com.overex.backend.event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.dto.responses.EventResponseDto;

@Mapper(componentModel = "spring")
public interface EventMapper {

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "status", constant = "DRAFT")
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "category", ignore = true)
        @Mapping(target = "createdBy", ignore = true)
        @Mapping(target = "version", ignore = true)
        Event toEntity(CreateEventDto dto);

        @Mapping(target = "categoryId", source = "category.id")
        CreateEventDto toCreateEventDto(Event event);

        EventResponseDto toEventResponseDto(Event event);

        @Mapping(target = "id", ignore = true)
        // Not copied blindly: EventService checks the transition and sets it.
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "category", ignore = true)
        @Mapping(target = "createdBy", ignore = true)
        // Only Hibernate changes the version; EventService compares it by hand.
        @Mapping(target = "version", ignore = true)
        void updateEntity(UpdateEventDto dto, @MappingTarget Event event);
}
