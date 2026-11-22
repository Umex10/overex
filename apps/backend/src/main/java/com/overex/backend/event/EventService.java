package com.overex.backend.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.dto.responses.CountsOfEventsDto;
import com.overex.backend.event.dto.responses.EventDetailsResponseDto;
import com.overex.backend.event.dto.responses.EventResponseDto;
import com.overex.backend.event.dto.responses.StatusOption;
import com.overex.backend.user.User;
import com.overex.backend.user.UserService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final EventMapper eventMapper;

    public Page<EventResponseDto> getEvents(UUID categoryId, Pageable pageable) {

        Page<Event> events = categoryId == null
                ? eventRepository.findAll(pageable)
                : eventRepository.findByCategoryId(categoryId, pageable);

        // Converts every event on the page; page number, size and total stay.
        return events.map(eventMapper::toEventResponseDto);
    }

    public Page<EventResponseDto> getMyEvents(UUID userId, UUID categoryId, MyEventsFilter filter,
            Pageable pageable) {

        userService.ensureUserExists(userId);

        MyEventCriteria myEventCriteria = MyEventCriteria.of(filter, Instant.now());

        return eventRepository.findMyEvents(userId,
                myEventCriteria.getStatus(),
                categoryId, myEventCriteria.getStartsAfter(), myEventCriteria.getStartsBeforeOrAt(), pageable)
                .map(eventMapper::toEventResponseDto);
    }

    public EventDetailsResponseDto getEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Event '%s' was not found", eventId)));

        Map<Status, StatusOption> statusOptions = EventStatusRules.statusOptions(event, Instant.now());

        EventResponseDto eventResponseDto = eventMapper.toEventResponseDto(event);

        return new EventDetailsResponseDto(eventResponseDto, statusOptions);
    }

    public CountsOfEventsDto getCountsOfEvents(UUID userId) {
        userService.ensureUserExists(userId);
        Instant now = Instant.now();

        return CountsOfEventsDto.builder()
                .countOfAllEvents(count(userId, MyEventsFilter.ALL, now))
                .countOfDraftEvents(count(userId, MyEventsFilter.DRAFT, now))
                .countOfPublishedEvents(count(userId, MyEventsFilter.PUBLISHED, now))
                .countOfCanceledEvents(count(userId, MyEventsFilter.CANCELED, now))
                .countOfFinishedEvents(count(userId, MyEventsFilter.FINISHED, now))
                .build();
    }

    public EventResponseDto createEvent(UUID userId, CreateEventDto createEventDto) {
        userService.ensureUserExists(userId);
        User user = userService.getReference(userId);
        Category category = categoryService.getCategory(createEventDto.categoryId());

        Event event = eventMapper.toEntity(createEventDto);
        event.setCategory(category);
        event.setCreatedBy(user);

        return eventMapper.toEventResponseDto(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDto updateEvent(UpdateEventDto updateEventDto, UUID eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Event '%s' was not found", eventId)));

        // Optimistic locking: findById loads the current version, so Hibernate's
        // own "WHERE version = ?" check would always pass. The version the
        // client loaded comes with the DTO, so it is compared here by hand.
        // Loaded version 3, stored version 3 → ok, nobody saved in between
        // Loaded version 3, stored version 4 → rejected with 409
        if (!Objects.equals(event.getVersion(), updateEventDto.version())) {
            throw new ObjectOptimisticLockingFailureException(Event.class, eventId);
        }

        Instant now = Instant.now();

        StatusOption statusOption = EventStatusRules.check(event, updateEventDto.status(), now);

        if (statusOption.disabled()) {
            throw fieldError("status", statusOption.why());
        }
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

        Category category = categoryService.getCategory(updateEventDto.categoryId());

        eventMapper.updateEntity(updateEventDto, event);
        event.setCategory(category);

        // saveAndFlush, not save: Hibernate raises the version and sets updatedAt
        // only when it writes to the database. With save() that happens at the
        // commit, after the response below was built, so the client would get the
        // old version back and its next save would fail with a false 409.
        return eventMapper.toEventResponseDto(eventRepository.saveAndFlush(event));
    }

    public void deleteEvent(UUID eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Event '%s' was not found", eventId)));
        eventRepository.delete(event);

    }

    private static OwnException fieldError(String field, String message) {
        return new OwnException(List.of(new ApiError.FieldError(field, message)));
    }

    /**
     * How many events the tab {@code filter} shows: the same query as the list,
     * only counted.
     */
    private long count(UUID userId, MyEventsFilter filter, Instant now) {
        MyEventCriteria c = MyEventCriteria.of(filter, now);
        return eventRepository
                .findMyEvents(userId, c.getStatus(), null, c.getStartsAfter(), c.getStartsBeforeOrAt(),
                        PageRequest.of(0, 1))
                .getTotalElements();
    }

}