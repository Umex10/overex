package com.overex.backend.event;

import java.util.List;
import java.util.UUID;

import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.dto.responses.EventDetailsResponseDto;
import com.overex.backend.event.dto.responses.EventResponseDto;
import org.springframework.data.domain.Sort;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventResponseDto>> getEvents(@RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 10, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<EventResponseDto> events = eventService.getEvents(categoryId, pageable);
        return new ResponseEntity<Page<EventResponseDto>>(events, HttpStatus.OK);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailsResponseDto> getEvent(@PathVariable UUID eventId) {
        EventDetailsResponseDto event = eventService.getEvent(eventId);
        return new ResponseEntity<EventDetailsResponseDto>(event, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEventDto createEventDto) {

        UUID userId = UUID.fromString(jwt.getSubject());

        EventResponseDto event = eventService.createEvent(userId, createEventDto);

        return new ResponseEntity<EventResponseDto>(event, HttpStatus.CREATED);

    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> updateEvent(@Valid @RequestBody UpdateEventDto updateEventDto,
            @PathVariable UUID eventId) {
        EventResponseDto event = eventService.updateEvent(updateEventDto, eventId);

        return new ResponseEntity<EventResponseDto>(event, HttpStatus.OK);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

}