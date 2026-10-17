package com.overex.backend.event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;

@Mapper(componentModel = "spring")
public interface EventMapper {

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "status", constant = "DRAFT")
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        Event toEntity(CreateEventDto dto);

        CreateEventDto toCreateEventDto(Event event);

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "status", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        void updateEntity(UpdateEventDto dto, @MappingTarget Event event);
}
