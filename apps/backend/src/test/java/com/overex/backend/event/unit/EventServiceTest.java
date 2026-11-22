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

import java.util.List;
import java.util.Map;
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

import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.Category;
import com.overex.backend.event.CategoryRepository;
import com.overex.backend.event.CategoryService;
import com.overex.backend.event.Event;
import com.overex.backend.event.EventMapper;
import com.overex.backend.event.EventRepository;
import com.overex.backend.event.EventService;
import com.overex.backend.event.EventStatusRules;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.dto.responses.CountsOfEventsDto;
import com.overex.backend.event.dto.responses.EventDetailsResponseDto;
import com.overex.backend.event.dto.responses.EventResponseDto;
import com.overex.backend.event.dto.responses.StatusOption;
import com.overex.backend.event.utils.EventTestDataFactory;
import com.overex.backend.event.utils.EventTestDataFactory.MyEventsFilterCase;
import com.overex.backend.event.utils.EventTestDataFactory.StatusRuleCase;
import com.overex.backend.user.UserRepository;
import com.overex.backend.user.UserService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link EventService}.
 *
 * <p>
 * Pure Mockito tests: no Spring context. Covers listing, create, get and
 * delete (including a missing event), and gives every business rule of the
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
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		CreateEventDto dto = EventTestDataFactory.createCreateEventDto();
		EventResponseDto eventResponseDto = EventTestDataFactory.createDefaultEventResponseDto();

		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventMapper.toEntity(dto)).thenReturn(event);
		when(eventRepository.save(event)).thenReturn(event);

		EventResponseDto createdEvent = eventService.createEvent(null, dto);

		assertEquals(eventResponseDto, createdEvent);
		assertSame(category.getId(), createdEvent.category().id());
		verify(eventMapper).toEntity(dto);
		verify(eventRepository).save(event);
	}

	/** All stored events are returned as they come from the repository. */
	@Test
	void shouldGetAllEvents() {
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		Page<Event> page = EventTestDataFactory.createEventPage();
		Page<EventResponseDto> pageToAssert = EventTestDataFactory.createEventResponsePage();

		when(eventRepository.findAll(pageable)).thenReturn(page);

		Page<EventResponseDto> foundPage = eventService.getEvents(null, pageable);

		assertEquals(pageToAssert, foundPage);
		verify(eventRepository, never()).findByCategoryId(any(), any());
	}

	@Test
	void shouldGetEventsWithCategory() {
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		Page<Event> page = EventTestDataFactory.createFestivalEventPage();
		Page<EventResponseDto> pageToAssert = EventTestDataFactory.createFestivalEventResponsePage();

		when(eventRepository.findByCategoryId(EventTestDataFactory.DEFAULT_CATEGORY_ID, pageable)).thenReturn(page);

		Page<EventResponseDto> foundPage = eventService.getEvents(EventTestDataFactory.DEFAULT_CATEGORY_ID, pageable);

		assertEquals(pageToAssert.getContent(), foundPage.getContent());
		verify(eventRepository, never()).findAll(any(Pageable.class));
	}

	/**
	 * Every tab asks the repository with the right criteria and returns the
	 * matching events as DTOs.
	 */
	@Test
	void shouldGetMyEventsForEveryTab() {
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		UUID userId = EventTestDataFactory.DEFAULT_USER_ID;

		for (MyEventsFilterCase tab : EventTestDataFactory.createMyEventsFilterCases()) {
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

			Page<EventResponseDto> foundPage = eventService.getMyEvents(userId, null, tab.filter(), pageable);

			assertEquals(pageToAssert, foundPage, "Tab " + tab.filter());
		}
	}

	/**
	 * Every tab asks the repository with the right criteria and returns the
	 * matching events as DTOs.
	 */
	@Test
	void shouldGetMyEventsCountsForEveryTab() {
		Pageable pageable = EventTestDataFactory.createCountPageable();
		UUID userId = EventTestDataFactory.DEFAULT_USER_ID;
		CountsOfEventsDto countsOfEventsDto = EventTestDataFactory.createCountsOfEventsDto();

		for (MyEventsFilterCase tab : EventTestDataFactory.createMyEventsFilterCases()) {
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

		CountsOfEventsDto foundCounts = eventService.getCountsOfEvents(userId);

		assertEquals(countsOfEventsDto, foundCounts);
	}

	@Test
	void shouldGetEvent() {
		Event event = EventTestDataFactory.createDefaultEvent();
		EventDetailsResponseDto eventDetailsResponseDto = EventTestDataFactory.createEventDetailsResponseDto();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		EventDetailsResponseDto foundEventDetailsResponseDto = eventService.getEvent(event.getId());

		assertEquals(eventDetailsResponseDto, foundEventDetailsResponseDto);
	}


	@Test
	void shouldNotGetMissingEvent() {
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
				() -> eventService.getEvent(event.getId()));

		assertEquals(String.format("Event '%s' was not found", event.getId()), ex.getMessage());
	}

	/** A valid update of an upcoming event is mapped onto the entity and saved. */
	@Test
	void shouldUpdateEvent() {
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDto();
		EventResponseDto dtoToAssert = EventTestDataFactory.createUpdatedEventResponseDto();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventRepository.saveAndFlush(event)).thenReturn(event);

		EventResponseDto updatedEvent = eventService.updateEvent(dto, event.getId());

		assertEquals(dtoToAssert, updatedEvent);
		assertSame(category.getId(), updatedEvent.category().id());
		verify(eventMapper).updateEntity(dto, event);
		verify(eventRepository).saveAndFlush(event);
	}

	/** An unchanged start time is not checked against "now" again. */
	@Test
	void shouldUpdateEventWithUnchangedStartsAt() {
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDtoWithUnchangedStartsAt();

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventRepository.saveAndFlush(event)).thenReturn(event);

		eventService.updateEvent(dto, event.getId());

		verify(eventMapper).updateEntity(dto, event);
		verify(eventRepository).saveAndFlush(event);
	}

	@Test
	void shouldNotUpdateMissingEvent() {
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDto(), event.getId()));

		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldNotUpdateEventBecauseVersionMismatch() {
		// someone else saved the event after the client loaded DEFAULT_VERSION
		Event eventWithOtherVersion = EventTestDataFactory.createDefaultEvent();
		eventWithOtherVersion.setVersion(EventTestDataFactory.OTHER_VERSION);

		when(eventRepository.findById(eventWithOtherVersion.getId())).thenReturn(Optional.of(eventWithOtherVersion));

		assertThrows(ObjectOptimisticLockingFailureException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDto(), eventWithOtherVersion.getId()));

		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	/** Every stored status and point in time gets the status options of the rules. */
	@Test
	void shouldGetStatusOptionsForEveryRuleCase() {
		for (StatusRuleCase ruleCase : EventTestDataFactory.createStatusRuleCases()) {
			Event event = ruleCase.event();

			when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

			EventDetailsResponseDto foundEvent = eventService.getEvent(event.getId());

			assertEquals(ruleCase.expectedOptions(), foundEvent.statusOptions(), ruleCase.name());
		}
	}

	/**
	 * Every status change the rules block is rejected on the field "status" with
	 * the reason of the rule, and nothing is saved.
	 */
	@Test
	void shouldNotUpdateEventWhenStatusRuleBlocksIt() {
		for (StatusRuleCase ruleCase : EventTestDataFactory.createStatusRuleCases()) {
			List<Map.Entry<Status, StatusOption>> blockedChanges = ruleCase.expectedOptions().entrySet().stream()
					.filter(option -> option.getValue().disabled())
					.toList();

			for (Map.Entry<Status, StatusOption> blockedChange : blockedChanges) {
				Event event = ruleCase.event();
				UpdateEventDto dto = EventTestDataFactory.createUpdateEventDtoWithStatus(blockedChange.getKey());

				when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

				OwnException ex = assertThrows(OwnException.class,
						() -> eventService.updateEvent(dto, event.getId()),
						ruleCase.name() + " → " + blockedChange.getKey());

				assertFieldError(ex, "status", blockedChange.getValue().why());
			}
		}

		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	/** An allowed status change goes through the update and is saved. */
	@Test
	void shouldUpdateEventStatusWhenRuleAllowsIt() {
		Event event = EventTestDataFactory.createDefaultEvent();
		Category category = EventTestDataFactory.createDefaultCategory();
		UpdateEventDto dto = EventTestDataFactory.createUpdateEventDtoWithStatus(Status.PUBLISHED);

		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(categoryService.getCategory(dto.categoryId())).thenReturn(category);
		when(eventRepository.saveAndFlush(event)).thenReturn(event);

		EventResponseDto updatedEvent = eventService.updateEvent(dto, event.getId());

		assertEquals(Status.PUBLISHED, updatedEvent.status());
		verify(eventRepository).saveAndFlush(event);
	}

	/** An event whose stored start lies in the past can't be changed anymore. */
	@Test
	void shouldNotUpdateStartedEvent() {
		Event startedEvent = EventTestDataFactory.createDefaultEvent();
		startedEvent.setStartsAt(EventTestDataFactory.PAST_STARTS_AT);
		startedEvent.setSaleStartsAt(EventTestDataFactory.PAST_SALE_STARTS_AT);

		when(eventRepository.findById(startedEvent.getId())).thenReturn(Optional.of(startedEvent));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDto(), startedEvent.getId()));

		assertFieldError(ex, "startsAt", "This event has already started and can't be updated anymore.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldNotUpdateEventWithStartsAtMovedToPast() {
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDtoWithPastStartsAt(),
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
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDtoWithSaleAtStart(),
						event.getId()));

		assertFieldError(ex, "saleStartsAt", "The sale must start before the event starts.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldNotUpdateEventWithSaleStartingAfterEventStart() {
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		OwnException ex = assertThrows(OwnException.class,
				() -> eventService.updateEvent(EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart(),
						event.getId()));

		assertFieldError(ex, "saleStartsAt", "The sale must start before the event starts.");
		verifyNoInteractions(eventMapper);
		verify(eventRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldDeleteEvent() {
		Event event = EventTestDataFactory.createDefaultEvent();
		when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

		eventService.deleteEvent(event.getId());

		verify(eventRepository).delete(event);
	}

	@Test
	void shouldNotDeleteMissingEvent() {
		Event event = EventTestDataFactory.createDefaultEvent();
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
