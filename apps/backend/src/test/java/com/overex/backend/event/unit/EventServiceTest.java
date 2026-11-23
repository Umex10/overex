package com.overex.backend.event.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.Category;
import com.overex.backend.event.CategoryService;
import com.overex.backend.event.Event;
import com.overex.backend.event.EventMapper;
import com.overex.backend.event.EventRepository;
import com.overex.backend.event.EventService;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.dto.responses.CountsOfEventsDto;
import com.overex.backend.event.dto.responses.EventDetailsResponseDto;
import com.overex.backend.event.dto.responses.EventResponseDto;
import com.overex.backend.event.utils.EventTestDataFactory;
import com.overex.backend.event.utils.EventTestDataFactory.MyEventsFilterCase;
import com.overex.backend.user.UserService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link EventService}.
 *
 * <p>
 * Pure Mockito tests: no Spring context. Covers listing, create, get and
 * delete (including a missing event and a stranger), and gives every business rule of the
 * update one payload that triggers it; a rejected update must neither map nor
 * save the event.
 *
 * <p>
 * Every test builds its own data in Arrange: the base objects come from
 * {@link EventTestDataFactory}, the one field that matters for the test is
 * changed right there.
 */
@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

	@Mock
	EventRepository eventRepository;

	@Mock
	UserService userService;

	@Spy
	EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

	@Mock
	CategoryService categoryService;

	@InjectMocks
	EventService eventService;

	/** The payload is mapped to an entity, gets its category and is saved. */
	@Test
	void shouldCreateEvent() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		CreateEventDto dto = EventTestDataFactory.createCreateEventDto();
		EventResponseDto eventResponseDto = EventTestDataFactory.createDefaultEventResponseDto();

		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventMapper.toEntity(dto)).thenReturn(event);
		when(eventRepository.save(event)).thenReturn(event);

		// Act
		EventResponseDto createdEvent = eventService.createEvent(null, dto);

		// Assert
		assertEquals(eventResponseDto, createdEvent);
		assertSame(category.getId(), createdEvent.category().id());
		verify(eventMapper).toEntity(dto);
		verify(eventRepository).save(event);
	}

	/** The published, upcoming events are returned as they come from the repository. */
	@Test
	void shouldGetAllEvents() {
		// Arrange
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		Page<Event> page = EventTestDataFactory.createEventPage();
		Page<EventResponseDto> pageToAssert = EventTestDataFactory.createEventResponsePage();

		when(eventRepository.findByStatusAndStartsAtAfter(eq(Status.PUBLISHED), notNull(), eq(pageable)))
				.thenReturn(page);

		// Act
		Page<EventResponseDto> foundPage = eventService.getEvents(null, pageable);

		// Assert
		assertEquals(pageToAssert, foundPage);
		verify(eventRepository, never()).findByCategoryIdAndStatusAndStartsAtAfter(any(), any(), any(), any());
	}

	@Test
	void shouldGetEventsWithCategory() {
		// Arrange
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		Page<Event> page = EventTestDataFactory.createFestivalEventPage();
		Page<EventResponseDto> pageToAssert = EventTestDataFactory.createFestivalEventResponsePage();

		when(eventRepository.findByCategoryIdAndStatusAndStartsAtAfter(eq(EventTestDataFactory.DEFAULT_CATEGORY_ID),
				eq(Status.PUBLISHED), notNull(), eq(pageable))).thenReturn(page);

		// Act
		Page<EventResponseDto> foundPage = eventService.getEvents(EventTestDataFactory.DEFAULT_CATEGORY_ID, pageable);

		// Assert
		assertEquals(pageToAssert.getContent(), foundPage.getContent());
		verify(eventRepository, never()).findByStatusAndStartsAtAfter(any(), any(), any());
	}

	/**
	 * Every tab asks the repository with the right criteria and returns the
	 * matching events as DTOs.
	 */
	@Test
	void shouldGetMyEventsForEveryTab() {
		// Arrange (shared by every tab)
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		UUID userId = EventTestDataFactory.DEFAULT_USER_ID;

		for (MyEventsFilterCase tab : EventTestDataFactory.createMyEventsFilterCases()) {
			// Arrange
			Page<Event> page = EventTestDataFactory.createMyEventPage(tab.filter(), pageable);
			Page<EventResponseDto> pageToAssert = EventTestDataFactory.createMyEventResponsePage(tab.filter(),
					pageable);

			when(eventRepository.findMyEvents(
					eq(userId),
					eq(tab.status()),
					isNull(),
					tab.startsAfterNow() ? notNull() : isNull(),
					tab.startsBeforeOrAtNow() ? notNull() : isNull(),
					eq(pageable)))
					.thenReturn(page);

			// Act
			Page<EventResponseDto> foundPage = eventService.getMyEvents(userId, null, tab.filter(), pageable);

			// Assert
			assertEquals(pageToAssert, foundPage, "Tab " + tab.filter());
		}
	}

	/**
	 * Every tab asks the repository with the right criteria and returns the
	 * matching events as DTOs.
	 */
	@Test
	void shouldGetMyEventsCountsForEveryTab() {
		// Arrange (shared by every tab)
		Pageable pageable = EventTestDataFactory.createCountPageable();
		UUID userId = EventTestDataFactory.DEFAULT_USER_ID;
		CountsOfEventsDto countsOfEventsDto = EventTestDataFactory.createCountsOfEventsDto();

		for (MyEventsFilterCase tab : EventTestDataFactory.createMyEventsFilterCases()) {
			// Arrange
			Page<Event> page = EventTestDataFactory.createMyEventPage(tab.filter(), pageable);

			when(eventRepository.findMyEvents(
					eq(userId),
					eq(tab.status()),
					isNull(),
					tab.startsAfterNow() ? notNull() : isNull(),
					tab.startsBeforeOrAtNow() ? notNull() : isNull(),
					eq(pageable)))
					.thenReturn(page);
		}

		// Act
		CountsOfEventsDto foundCounts = eventService.getCountsOfEvents(userId);

		// Assert
		assertEquals(countsOfEventsDto, foundCounts);
	}

	@Test
	void shouldGetEvent() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		EventDetailsResponseDto eventDetailsResponseDto = EventTestDataFactory.createEventDetailsResponseDto();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		// Act
		EventDetailsResponseDto foundEventDetailsResponseDto = eventService.getEvent(EventTestDataFactory.DEFAULT_USER_ID, event.getId());

		// Assert
		assertEquals(eventDetailsResponseDto, foundEventDetailsResponseDto);
	}

	@Test
	void shouldNotGetMissingEvent() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		// Act + Assert (assertThrows runs the call and checks the exception)
		EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
				() -> eventService.getEvent(EventTestDataFactory.DEFAULT_USER_ID, event.getId()));

		assertEquals(String.format("Event '%s' was not found", event.getId()), ex.getMessage());
	}

	/** A valid update of an upcoming event is mapped onto the entity and saved. */
	@Test
	void shouldUpdateEvent() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDto();
		EventResponseDto dtoToAssert = EventTestDataFactory.createUpdatedEventResponseDto();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventRepository.saveAndFlush(event)).thenReturn(event);

		// Act
		EventResponseDto updatedEvent = eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, dto, event.getId());

		// Assert
		assertEquals(dtoToAssert, updatedEvent);
		assertSame(category.getId(), updatedEvent.category().id());
		verify(eventMapper).updateEntity(dto, event);
		verify(eventRepository).saveAndFlush(event);
	}

	/** An unchanged start time is not checked against "now" again. */
	@Test
	void shouldUpdateEventWithUnchangedStartsAt() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDtoWithUnchangedStartsAt();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventRepository.saveAndFlush(event)).thenReturn(event);

		// Act
		eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, dto, event.getId());

		// Assert
		verify(eventMapper).updateEntity(dto, event);
		verify(eventRepository).saveAndFlush(event);
	}

	@Test
	void shouldNotUpdateMissingEvent() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		// Act + Assert (assertThrows runs the call and checks the exception)
		assertThrows(EntityNotFoundException.class,
				() -> eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, EventTestDataFactory.createUpdateEventDto(), event.getId()));

		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	/** Only the creator may change the event, everyone else gets 403. */
	@Test
	void shouldNotUpdateEventOfAnotherUser() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDto();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		// Act + Assert (assertThrows runs the call and checks the exception)
		assertThrows(AccessDeniedException.class,
				() -> eventService.updateEvent(EventTestDataFactory.OTHER_USER_ID, dto, event.getId()));

		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldNotUpdateEventBecauseVersionMismatch() {
		// Arrange
		// someone else saved the event after the client loaded DEFAULT_VERSION
		Event eventWithOtherVersion = EventTestDataFactory.createDefaultEvent();
		eventWithOtherVersion.setVersion(EventTestDataFactory.OTHER_VERSION);

		when(eventRepository.findById(eventWithOtherVersion.getId())).thenReturn(Optional.of(eventWithOtherVersion));

		// Act + Assert (assertThrows runs the call and checks the exception)
		assertThrows(ObjectOptimisticLockingFailureException.class,
				() -> eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, EventTestDataFactory.createUpdateEventDto(), eventWithOtherVersion.getId()));

		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	/**
	 * A status change the rules block is rejected on the field "status" with the
	 * reason of the rule, and nothing is saved. All rule cases are covered by
	 * {@link EventStatusRulesTest}.
	 */
	@Test
	void shouldNotUpdateEventWhenStatusRuleBlocksIt() {
		// Arrange
		Event draft = EventTestDataFactory.createDefaultEvent();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDtoWithStatus(Status.CANCELED);

		when(eventRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		// Act + Assert (assertThrows runs the call and checks the exception)
		OwnException ex = assertThrows(OwnException.class, () -> eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, dto, draft.getId()));

		assertFieldError(ex, "status", EventTestDataFactory.DRAFT_CANCEL_BLOCKED_WHY);
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	/** An allowed status change goes through the update and is saved. */
	@Test
	void shouldUpdateEventStatusWhenRuleAllowsIt() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDtoWithStatus(Status.PUBLISHED);

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventRepository.saveAndFlush(event)).thenReturn(event);

		// Act
		EventResponseDto updatedEvent = eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, dto, event.getId());

		// Assert
		assertEquals(Status.PUBLISHED, updatedEvent.status());
		verify(eventRepository).saveAndFlush(event);
	}

	/** An event whose stored start lies in the past can't be changed anymore. */
	@Test
	void shouldNotUpdateStartedEvent() {
		// Arrange
		Event startedEvent = EventTestDataFactory.createDefaultEvent();
		startedEvent.setStartsAt(EventTestDataFactory.PAST_STARTS_AT);
		startedEvent.setSaleStartsAt(EventTestDataFactory.PAST_SALE_STARTS_AT);

		when(eventRepository.findById(startedEvent.getId())).thenReturn(Optional.of(startedEvent));

		// Act + Assert (assertThrows runs the call and checks the exception)
		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, EventTestDataFactory.createUpdateEventDto(), startedEvent.getId()));

		assertFieldError(ex, "startsAt", "This event has already started and can't be updated anymore.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldNotUpdateEventWithStartsAtMovedToPast() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		// Act + Assert (assertThrows runs the call and checks the exception)
		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, EventTestDataFactory.createUpdateEventDtoWithPastStartsAt(),
						event.getId()));

		assertFieldError(ex, "startsAt", "The new start time must be in the future.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	/**
	 * Boundary: a sale that opens exactly when the event starts is rejected too.
	 */
	@Test
	void shouldNotUpdateEventWithSaleStartingAtEventStart() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		// Act + Assert (assertThrows runs the call and checks the exception)
		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, EventTestDataFactory.createUpdateEventDtoWithSaleAtStart(),
						event.getId()));

		assertFieldError(ex, "saleStartsAt", "The sale must start before the event starts.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldNotUpdateEventWithSaleStartingAfterEventStart() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		// Act + Assert (assertThrows runs the call and checks the exception)
		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.DEFAULT_USER_ID, EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart(),
						event.getId()));

		assertFieldError(ex, "saleStartsAt", "The sale must start before the event starts.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldDeleteEvent() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		// Act
		eventService.deleteEvent(EventTestDataFactory.DEFAULT_USER_ID, event.getId());

		// Assert
		verify(eventRepository).delete(event);
	}

	@Test
	void shouldNotDeleteMissingEvent() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		// Act + Assert (assertThrows runs the call and checks the exception)
		EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
				() -> eventService.deleteEvent(EventTestDataFactory.DEFAULT_USER_ID, event.getId()));

		assertEquals(String.format("Event '%s' was not found", event.getId()), ex.getMessage());
		verify(eventRepository, never()).delete(any());
	}

	/** Only the creator may delete the event, everyone else gets 403. */
	@Test
	void shouldNotDeleteEventOfAnotherUser() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		// Act + Assert (assertThrows runs the call and checks the exception)
		assertThrows(AccessDeniedException.class,
				() -> eventService.deleteEvent(EventTestDataFactory.OTHER_USER_ID, event.getId()));

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
