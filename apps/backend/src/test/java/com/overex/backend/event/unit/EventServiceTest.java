package com.overex.backend.event.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.Event;
import com.overex.backend.event.EventMapper;
import com.overex.backend.event.EventRepository;
import com.overex.backend.event.EventService;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.utils.EventTestDataFactory;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link EventService}.
 *
 * <p>
 * Pure Mockito tests: no Spring context. Covers create, get and delete
 * (including a missing event), and gives every business rule of the update one
 * payload that triggers it; a rejected update must neither map nor save the
 * event.
 */
@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

	@Mock
	EventRepository eventRepository;

	@Mock
	EventMapper eventMapper;

	@InjectMocks
	EventService eventService;

	private final Event event = EventTestDataFactory.createDefaultEvent();

	/** The payload is mapped to an entity and that entity is saved. */
	@Test
	void shouldCreateEvent() {
		CreateEventDto dto = EventTestDataFactory.createCreateEventDto();

		when(eventMapper.toEntity(dto)).thenReturn(event);
		when(eventRepository.save(event)).thenReturn(event);

		Event createdEvent = eventService.createEvent(dto);

		assertSame(event, createdEvent);
		verify(eventMapper).toEntity(dto);
		verify(eventRepository).save(event);
	}

	@Test
	void shouldGetEvent() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		Event foundEvent = eventService.getEvent(event.getId());

		assertSame(event, foundEvent);
	}

	@Test
	void shouldNotGetMissingEvent() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
				() -> eventService.getEvent(event.getId()));

		assertEquals(String.format("Event '%s' was not found", event.getId()), ex.getMessage());
	}

	/** A valid update of an upcoming event is mapped onto the entity and saved. */
	@Test
	void shouldUpdateEvent() {
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDto();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.save(event)).thenReturn(event);

		Event updatedEvent = eventService.updateEvent(dto, event.getId());

		assertSame(event, updatedEvent);
		verify(eventMapper).updateEntity(dto, event);
		verify(eventRepository).save(event);
	}

	/** An unchanged start time is not checked against "now" again. */
	@Test
	void shouldUpdateEventWithUnchangedStartsAt() {
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDtoWithUnchangedStartsAt();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(eventRepository.save(event)).thenReturn(event);

		eventService.updateEvent(dto, event.getId());

		verify(eventMapper).updateEntity(dto, event);
		verify(eventRepository).save(event);
	}

	@Test
	void shouldNotUpdateMissingEvent() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDto(), event.getId()));

		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).save(any());
	}

	/** An event whose stored start lies in the past can't be changed anymore. */
	@Test
	void shouldNotUpdateStartedEvent() {
		Event startedEvent = EventTestDataFactory.createStartedEvent();

		when(eventRepository.findById(startedEvent.getId())).thenReturn(Optional.of(startedEvent));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDto(), startedEvent.getId()));

		assertFieldError(ex, "startsAt", "This event has already started and can't be updated anymore.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).save(any());
	}

	@Test
	void shouldNotUpdateEventWithStartsAtMovedToPast() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDtoWithPastStartsAt(),
						event.getId()));

		assertFieldError(ex, "startsAt", "The new start time must be in the future.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).save(any());
	}

	/** Boundary: a sale that opens exactly when the event starts is rejected too. */
	@Test
	void shouldNotUpdateEventWithSaleStartingAtEventStart() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDtoWithSaleAtStart(),
						event.getId()));

		assertFieldError(ex, "saleStartsAt", "The sale must start before the event starts.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).save(any());
	}

	@Test
	void shouldNotUpdateEventWithSaleStartingAfterEventStart() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart(),
						event.getId()));

		assertFieldError(ex, "saleStartsAt", "The sale must start before the event starts.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).save(any());
	}

	@Test
	void shouldDeleteEvent() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		eventService.deleteEvent(event.getId());

		verify(eventRepository).delete(event);
	}

	@Test
	void shouldNotDeleteMissingEvent() {
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
				() -> eventService.deleteEvent(event.getId()));

		assertEquals(String.format("Event '%s' was not found", event.getId()), ex.getMessage());
		verify(eventRepository, never()).delete(any());
	}

	/** Asserts that the exception carries exactly one error for the given field. */
	private static void assertFieldError(OwnException ex, String field, String message) {
		assertEquals(1, ex.getErrors().size());
		ApiError.FieldError error = ex.getErrors().get(0);
		assertEquals(field, error.getField());
		assertEquals(message, error.getMessage());
	}
}
