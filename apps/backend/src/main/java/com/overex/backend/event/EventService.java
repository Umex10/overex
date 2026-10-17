package com.overex.backend.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    public Event getEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Event '%s' was not found", eventId)));
    }

    public Event createEvent(CreateEventDto createEventDto) {

        Event event = eventMapper.toEntity(createEventDto);

        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(UpdateEventDto updateEventDto, UUID eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Event '%s' was not found", eventId)));

        Instant now = Instant.now();

        // All checks run before the mapping: first validate, then change the event.

        // Today is Aug 1:
        // stored startsAt Aug 12 → ok, the event is still ahead
        // stored startsAt Jul 20 → rejected, the event is already over
        if (!event.getStartsAt().isAfter(now)) {
            throw fieldError("startsAt", "This event has already started and can't be updated anymore.");
        }

        // Today is Aug 1, stored startsAt Aug 12:
        // new startsAt Aug 12 (unchanged) → ok, nothing to check
        // new startsAt Aug 10 (moved earlier) → ok, still in the future
        // new startsAt Jul 30 (moved to past) → rejected
        boolean startsAtChanged = !updateEventDto.startsAt().equals(event.getStartsAt());
        if (startsAtChanged && !updateEventDto.startsAt().isAfter(now)) {
            throw fieldError("startsAt", "The new start time must be in the future.");
        }

        // The sale must open before the event starts (today's date doesn't matter
        // here).
        // new startsAt Aug 12:
        // new saleStartsAt Jul 1 → ok, sale opens before the event
        // new saleStartsAt Aug 12 → rejected, sale would open as the event starts
        // new saleStartsAt Aug 15 → rejected, sale would open after the event
        if (!updateEventDto.saleStartsAt().isBefore(updateEventDto.startsAt())) {
            throw fieldError("saleStartsAt", "The sale must start before the event starts.");
        }

        eventMapper.updateEntity(updateEventDto, event);

        return eventRepository.save(event);
    }

    public void deleteEvent(UUID eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Event '%s' was not found", eventId)));
        eventRepository.delete(event);

    }

    private static OwnException fieldError(String field, String message) {
        return new OwnException(List.of(new ApiError.FieldError(field, message)));
    }

}