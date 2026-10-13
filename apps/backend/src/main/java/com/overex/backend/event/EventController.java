package com.overex.backend.event;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/event")
public class EventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<Event> getEvent(@PathVariable UUID eventId) {
        Event event = eventService.getEvent(eventId);
        return new ResponseEntity<Event>(event, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@Valid @RequestBody CreateEventDto createEventDto) {

        Event event = eventService.createEvent(createEventDto);

        return new ResponseEntity<Event>(event, HttpStatus.CREATED);

    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<Event> updateEvent(@Valid @RequestBody UpdateEventDto updateEventDto,
            @PathVariable UUID eventId) {
        Event event = eventService.updateEvent(updateEventDto, eventId);

        return new ResponseEntity<Event>(event, HttpStatus.OK);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

}
