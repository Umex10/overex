package com.overex.backend.event.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.overex.backend.auth.utils.AuthTestDataFactory;
import com.overex.backend.event.Category;
import com.overex.backend.event.Event;
import com.overex.backend.event.MyEventsFilter;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.dto.responses.CategoryResponseDto;
import com.overex.backend.event.dto.responses.CountsOfEventsDto;
import com.overex.backend.event.dto.responses.EventDetailsResponseDto;
import com.overex.backend.event.dto.responses.EventResponseDto;
import com.overex.backend.event.dto.responses.StatusOption;

public final class EventTestDataFactory {

	private EventTestDataFactory() {
	}

	public static final UUID DEFAULT_EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	// The category every event and payload refers to.
	public static final UUID DEFAULT_CATEGORY_ID = CategoryTestDataFactory.DEFAULT_CATEGORY_ID;
	public static final String DEFAULT_CATEGORY_TITLE = CategoryTestDataFactory.DEFAULT_CATEGORY_TITLE;

	// A second category, so the list can be filtered (only the second event belongs
	// to it).
	public static final UUID CONCERT_CATEGORY_ID = CategoryTestDataFactory.CONCERT_CATEGORY_ID;
	public static final String CONCERT_CATEGORY_TITLE = CategoryTestDataFactory.CONCERT_CATEGORY_TITLE;

	// The same paging defaults as @PageableDefault on EventController#getEvents.
	public static final int DEFAULT_PAGE = 0;
	public static final int DEFAULT_PAGE_SIZE = 10;
	public static final String DEFAULT_SORT_PROPERTY = "startsAt";

	// Values of the default event and the create payload.
	public static final String DEFAULT_TITLE = "Summer Festival";
	public static final String DEFAULT_DESCRIPTION = "Open-air festival with live bands.";
	public static final String DEFAULT_VENUE = "Donauinsel";
	public static final String DEFAULT_CITY = "Wien";
	public static final Instant DEFAULT_STARTS_AT = Instant.parse("2099-06-20T19:00:00Z");
	public static final Instant DEFAULT_SALE_STARTS_AT = Instant.parse("2099-03-01T10:00:00Z");
	public static final String DEFAULT_IMAGE_URL = "https://example.com/images/summer-festival.png";
	public static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-01T12:00:00Z");
	public static final Long DEFAULT_VERSION = 0L;
	public static final Long OTHER_VERSION = DEFAULT_VERSION + 1;

	// Values of the update payload; each one differs from its default counterpart.
	public static final String UPDATED_TITLE = "Winter Festival";
	public static final String UPDATED_DESCRIPTION = "Indoor festival with DJs.";
	public static final String UPDATED_VENUE = "Stadthalle";
	public static final String UPDATED_CITY = "Graz";
	public static final Instant UPDATED_STARTS_AT = Instant.parse("2099-12-10T20:00:00Z");
	public static final Instant UPDATED_SALE_STARTS_AT = Instant.parse("2099-09-01T10:00:00Z");
	public static final String UPDATED_IMAGE_URL = "https://example.com/images/winter-festival.png";

	// Points in time that are already over.
	public static final Instant PAST_STARTS_AT = Instant.parse("2020-06-20T19:00:00Z");
	public static final Instant PAST_SALE_STARTS_AT = Instant.parse("2020-03-01T10:00:00Z");

	// Values of the second event of the list; every field differs from the default
	// event.
	public static final UUID SECOND_EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	public static final String SECOND_TITLE = "Jazz Night";
	public static final String SECOND_DESCRIPTION = "An evening of live jazz.";
	public static final String SECOND_VENUE = "Porgy & Bess";
	public static final String SECOND_CITY = "Wien";
	public static final Instant SECOND_STARTS_AT = Instant.parse("2099-08-15T20:00:00Z");
	public static final Instant SECOND_SALE_STARTS_AT = Instant.parse("2099-05-01T09:00:00Z");
	public static final String SECOND_IMAGE_URL = "https://example.com/images/jazz-night.png";

	// Values of the third event of the list; every field differs from the other
	// two.
	public static final UUID THIRD_EVENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	public static final String THIRD_TITLE = "Rock am Berg";
	public static final String THIRD_DESCRIPTION = "Two days of rock music in the mountains.";
	public static final String THIRD_VENUE = "Festivalgelände Mittersill";
	public static final String THIRD_CITY = "Salzburg";
	public static final Instant THIRD_STARTS_AT = Instant.parse("2099-07-04T16:00:00Z");
	public static final Instant THIRD_SALE_STARTS_AT = Instant.parse("2099-01-15T08:00:00Z");
	public static final String THIRD_IMAGE_URL = "https://example.com/images/rock-am-berg.png";

	// Values of the finished event of "My events": published, but its start lies
	// in the past. Only part of createMyEventList(), not of createEventList().
	public static final UUID FINISHED_EVENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
	public static final String FINISHED_TITLE = "Herbstkonzert";
	public static final String FINISHED_DESCRIPTION = "Classical concert in the old town hall.";
	public static final String FINISHED_VENUE = "Alte Stadthalle";
	public static final String FINISHED_CITY = "Linz";
	public static final String FINISHED_IMAGE_URL = "https://example.com/images/herbstkonzert.png";

	public static final UUID DEFAULT_USER_ID = AuthTestDataFactory.DEFAULT_USER_ID;

	// Reasons of EventStatusRules for the changes that are blocked even far before
	// the start (the default event starts in 2099).
	public static final String DRAFT_CANCEL_BLOCKED_WHY = "A draft was never public, so there is nothing to cancel. Delete it instead.";
	public static final String CANCELED_PUBLISH_BLOCKED_WHY = "A canceled event can't be published again.";
	public static final String CANCELED_DRAFT_BLOCKED_WHY = "A canceled event can't be turned back into a draft.";
	// Reasons that only show up once the start or the sale lies in the past.
	public static final String STARTED_PUBLISH_BLOCKED_WHY = "This event has already started, so it can't be published anymore.";
	public static final String STARTED_CANCEL_BLOCKED_WHY = "This event has already started, so it can't be canceled anymore.";
	public static final String SALE_STARTED_DRAFT_BLOCKED_WHY = "The ticket sale has already started, so the event can't go back to draft.";

	// ---------------------------------------------------------------------------
	// Categories
	// ---------------------------------------------------------------------------

	public static Category createDefaultCategory() {
		return CategoryTestDataFactory.createDefaultCategory();
	}

	public static Category createConcertCategory() {
		return CategoryTestDataFactory.createConcertCategory();
	}

	public static Category createUnsavedCategory(String title) {
		return CategoryTestDataFactory.createUnsavedCategory(title);
	}

	// ---------------------------------------------------------------------------
	// Entities (what the repository returns)
	// ---------------------------------------------------------------------------

	public static Event createDefaultEvent() {
		return Event.builder()
				.id(DEFAULT_EVENT_ID)
				.version(DEFAULT_VERSION)
				.category(createDefaultCategory())
				.title(DEFAULT_TITLE)
				.description(DEFAULT_DESCRIPTION)
				.venue(DEFAULT_VENUE)
				.city(DEFAULT_CITY)
				.startsAt(DEFAULT_STARTS_AT)
				.saleStartsAt(DEFAULT_SALE_STARTS_AT)
				.imageUrl(DEFAULT_IMAGE_URL)
				.status(Status.DRAFT)
				.createdAt(DEFAULT_CREATED_AT)
				.updatedAt(DEFAULT_CREATED_AT)
				.build();
	}

	public static Event createUpdatedEvent() {
		Event event = createDefaultEvent();
		event.setTitle(UPDATED_TITLE);
		event.setDescription(UPDATED_DESCRIPTION);
		event.setVenue(UPDATED_VENUE);
		event.setCity(UPDATED_CITY);
		event.setStartsAt(UPDATED_STARTS_AT);
		event.setSaleStartsAt(UPDATED_SALE_STARTS_AT);
		event.setImageUrl(UPDATED_IMAGE_URL);
		return event;
	}

	public static List<Event> createEventList() {
		Event secondEvent = Event.builder()
				.id(SECOND_EVENT_ID)
				.version(DEFAULT_VERSION)
				.category(createConcertCategory())
				.title(SECOND_TITLE)
				.description(SECOND_DESCRIPTION)
				.venue(SECOND_VENUE)
				.city(SECOND_CITY)
				.startsAt(SECOND_STARTS_AT)
				.saleStartsAt(SECOND_SALE_STARTS_AT)
				.imageUrl(SECOND_IMAGE_URL)
				.status(Status.PUBLISHED)
				.createdAt(DEFAULT_CREATED_AT)
				.updatedAt(DEFAULT_CREATED_AT)
				.build();

		Event thirdEvent = Event.builder()
				.id(THIRD_EVENT_ID)
				.version(DEFAULT_VERSION)
				.category(createDefaultCategory())
				.title(THIRD_TITLE)
				.description(THIRD_DESCRIPTION)
				.venue(THIRD_VENUE)
				.city(THIRD_CITY)
				.startsAt(THIRD_STARTS_AT)
				.saleStartsAt(THIRD_SALE_STARTS_AT)
				.imageUrl(THIRD_IMAGE_URL)
				.status(Status.CANCELED)
				.createdAt(DEFAULT_CREATED_AT)
				.updatedAt(DEFAULT_CREATED_AT)
				.build();

		return List.of(createDefaultEvent(), secondEvent, thirdEvent);
	}

	// ---------------------------------------------------------------------------
	// Paging
	// ---------------------------------------------------------------------------

	public static Pageable createDefaultPageable() {
		return createPageable(DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
	}

	public static Pageable createPageable(int page, int size) {
		return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, DEFAULT_SORT_PROPERTY));
	}

	// ---------------------------------------------------------------------------
	// Entity pages (what the repository returns)
	// ---------------------------------------------------------------------------

	public static Page<Event> createEventPage() {
		return createEventPage(null, createDefaultPageable());
	}

	public static Page<Event> createFestivalEventPage() {
		return createEventPage(DEFAULT_CATEGORY_ID, createDefaultPageable());
	}

	public static Page<Event> createEventPage(UUID categoryId, Pageable pageable) {
		List<Event> matching = createEventList().stream()
				.filter(event -> categoryId == null || categoryId.equals(event.getCategory().getId()))
				.sorted(Comparator.comparing(Event::getStartsAt))
				.toList();

		int from = (int) Math.min(pageable.getOffset(), matching.size());
		int to = Math.min(from + pageable.getPageSize(), matching.size());

		return new PageImpl<>(matching.subList(from, to), pageable, matching.size());
	}

	// ---------------------------------------------------------------------------
	// Response DTOs (what the service returns and the controller sends)
	// ---------------------------------------------------------------------------

	public static EventResponseDto createDefaultEventResponseDto() {
		return createDefaultEventResponseDto(Status.DRAFT);
	}

	public static EventResponseDto createDefaultEventResponseDto(Status status) {
		Event event = createDefaultEvent();
		event.setStatus(status);
		return toEventResponseDto(event);
	}

	public static EventResponseDto createUpdatedEventResponseDto() {
		return toEventResponseDto(createUpdatedEvent());
	}

	public static EventDetailsResponseDto createEventDetailsResponseDto() {
		return createEventDetailsResponseDto(Status.DRAFT);
	}

	// GET /events/{id} for the default event (starts 2099, sale 2099) in the given
	// status. Written out by hand, not with EventStatusRules:
	//
	// stored \ to   DRAFT     PUBLISHED   CANCELED
	// DRAFT         allowed   allowed     blocked
	// PUBLISHED     allowed   allowed     allowed
	// CANCELED      blocked   blocked     allowed
	public static EventDetailsResponseDto createEventDetailsResponseDto(Status status) {
		Map<Status, StatusOption> statusOptions = new EnumMap<>(Status.class);

		switch (status) {
			case DRAFT -> {
				statusOptions.put(Status.DRAFT, StatusOption.allowed());
				statusOptions.put(Status.PUBLISHED, StatusOption.allowed());
				statusOptions.put(Status.CANCELED, StatusOption.blocked(DRAFT_CANCEL_BLOCKED_WHY));
			}
			case PUBLISHED -> {
				statusOptions.put(Status.DRAFT, StatusOption.allowed());
				statusOptions.put(Status.PUBLISHED, StatusOption.allowed());
				statusOptions.put(Status.CANCELED, StatusOption.allowed());
			}
			case CANCELED -> {
				statusOptions.put(Status.DRAFT, StatusOption.blocked(CANCELED_DRAFT_BLOCKED_WHY));
				statusOptions.put(Status.PUBLISHED, StatusOption.blocked(CANCELED_PUBLISH_BLOCKED_WHY));
				statusOptions.put(Status.CANCELED, StatusOption.allowed());
			}
		}

		return new EventDetailsResponseDto(createDefaultEventResponseDto(status), statusOptions);
	}

	// ---------------------------------------------------------------------------
	// Status rules (EventStatusRules, reached through getEvent and updateEvent)
	// ---------------------------------------------------------------------------

	// One stored event and the status options it must get. The dates lie in 2020
	// (over) or 2099 (ahead), so EventService's real Instant.now() always decides
	// the same way.
	public record StatusRuleCase(String name, Event event, Map<Status, StatusOption> expectedOptions) {
	}

	// stored status, start, sale         → to DRAFT       to PUBLISHED    to CANCELED
	public static List<StatusRuleCase> createStatusRuleCases() {
		return List.of(
				new StatusRuleCase("DRAFT, start and sale ahead",
						createEventWith(Status.DRAFT, DEFAULT_STARTS_AT, DEFAULT_SALE_STARTS_AT),
						createStatusOptions(
								StatusOption.allowed(),
								StatusOption.allowed(),
								StatusOption.blocked(DRAFT_CANCEL_BLOCKED_WHY))),

				new StatusRuleCase("DRAFT, already started",
						createEventWith(Status.DRAFT, PAST_STARTS_AT, PAST_SALE_STARTS_AT),
						createStatusOptions(
								StatusOption.allowed(),
								StatusOption.blocked(STARTED_PUBLISH_BLOCKED_WHY),
								StatusOption.blocked(DRAFT_CANCEL_BLOCKED_WHY))),

				new StatusRuleCase("PUBLISHED, start and sale ahead",
						createEventWith(Status.PUBLISHED, DEFAULT_STARTS_AT, DEFAULT_SALE_STARTS_AT),
						createStatusOptions(
								StatusOption.allowed(),
								StatusOption.allowed(),
								StatusOption.allowed())),

				new StatusRuleCase("PUBLISHED, sale open, start ahead",
						createEventWith(Status.PUBLISHED, DEFAULT_STARTS_AT, PAST_SALE_STARTS_AT),
						createStatusOptions(
								StatusOption.blocked(SALE_STARTED_DRAFT_BLOCKED_WHY),
								StatusOption.allowed(),
								StatusOption.allowed())),

				new StatusRuleCase("PUBLISHED, already started",
						createEventWith(Status.PUBLISHED, PAST_STARTS_AT, PAST_SALE_STARTS_AT),
						createStatusOptions(
								StatusOption.blocked(SALE_STARTED_DRAFT_BLOCKED_WHY),
								StatusOption.allowed(),
								StatusOption.blocked(STARTED_CANCEL_BLOCKED_WHY))),

				new StatusRuleCase("CANCELED, start and sale ahead",
						createEventWith(Status.CANCELED, DEFAULT_STARTS_AT, DEFAULT_SALE_STARTS_AT),
						createStatusOptions(
								StatusOption.blocked(CANCELED_DRAFT_BLOCKED_WHY),
								StatusOption.blocked(CANCELED_PUBLISH_BLOCKED_WHY),
								StatusOption.allowed())),

				new StatusRuleCase("CANCELED, already started",
						createEventWith(Status.CANCELED, PAST_STARTS_AT, PAST_SALE_STARTS_AT),
						createStatusOptions(
								StatusOption.blocked(CANCELED_DRAFT_BLOCKED_WHY),
								StatusOption.blocked(CANCELED_PUBLISH_BLOCKED_WHY),
								StatusOption.allowed())));
	}

	private static Event createEventWith(Status status, Instant startsAt, Instant saleStartsAt) {
		Event event = createDefaultEvent();
		event.setStatus(status);
		event.setStartsAt(startsAt);
		event.setSaleStartsAt(saleStartsAt);
		return event;
	}

	private static Map<Status, StatusOption> createStatusOptions(StatusOption toDraft, StatusOption toPublished,
			StatusOption toCanceled) {
		Map<Status, StatusOption> statusOptions = new EnumMap<>(Status.class);
		statusOptions.put(Status.DRAFT, toDraft);
		statusOptions.put(Status.PUBLISHED, toPublished);
		statusOptions.put(Status.CANCELED, toCanceled);
		return statusOptions;
	}

	public static Page<EventResponseDto> createEventResponsePage() {
		return createEventResponsePage(null, createDefaultPageable());
	}

	public static Page<EventResponseDto> createFestivalEventResponsePage() {
		return createEventResponsePage(DEFAULT_CATEGORY_ID, createDefaultPageable());
	}

	public static Page<EventResponseDto> createEventResponsePage(UUID categoryId, Pageable pageable) {
		return createEventPage(categoryId, pageable).map(EventTestDataFactory::toEventResponseDto);
	}

	// ---------------------------------------------------------------------------
	// My events (GET /me/events and /me/events/counts)
	// ---------------------------------------------------------------------------

	// What getMyEvents must pass to findMyEvents for one tab (see MyEventCriteria#of).
	// The service uses Instant.now(), so the flags only say "set" (notNull) or "null".
	public record MyEventsFilterCase(MyEventsFilter filter, Status status, boolean startsAfterNow,
			boolean startsBeforeOrAtNow, long expectedCount) {
	}

	public static List<MyEventsFilterCase> createMyEventsFilterCases() {
		return List.of(
				new MyEventsFilterCase(MyEventsFilter.ALL, null, false, false, 4),
				new MyEventsFilterCase(MyEventsFilter.DRAFT, Status.DRAFT, false, false, 1),
				new MyEventsFilterCase(MyEventsFilter.PUBLISHED, Status.PUBLISHED, true, false, 1),
				new MyEventsFilterCase(MyEventsFilter.CANCELED, Status.CANCELED, false, false, 1),
				new MyEventsFilterCase(MyEventsFilter.FINISHED, Status.PUBLISHED, false, true, 1));
	}

	public static Event createFinishedEvent() {
		return Event.builder()
				.id(FINISHED_EVENT_ID)
				.version(DEFAULT_VERSION)
				.category(createConcertCategory())
				.title(FINISHED_TITLE)
				.description(FINISHED_DESCRIPTION)
				.venue(FINISHED_VENUE)
				.city(FINISHED_CITY)
				.startsAt(PAST_STARTS_AT)
				.saleStartsAt(PAST_SALE_STARTS_AT)
				.imageUrl(FINISHED_IMAGE_URL)
				.status(Status.PUBLISHED)
				.createdAt(DEFAULT_CREATED_AT)
				.updatedAt(DEFAULT_CREATED_AT)
				.build();
	}

	public static List<Event> createMyEventList() {
		List<Event> events = new ArrayList<>(createEventList());
		events.add(createFinishedEvent());
		return events;
	}

	public static Page<Event> createMyEventPage() {
		return createMyEventPage(MyEventsFilter.ALL, createDefaultPageable());
	}

	public static Page<Event> createMyEventPage(MyEventsFilter filter, Pageable pageable) {
		Instant now = Instant.now();
		List<Event> matching = createMyEventList().stream()
				.filter(event -> switch (filter) {
					case ALL -> true;
					case DRAFT -> event.getStatus() == Status.DRAFT;
					case CANCELED -> event.getStatus() == Status.CANCELED;
					case PUBLISHED -> event.getStatus() == Status.PUBLISHED && event.getStartsAt().isAfter(now);
					case FINISHED -> event.getStatus() == Status.PUBLISHED && !event.getStartsAt().isAfter(now);
				})
				.sorted(Comparator.comparing(Event::getStartsAt))
				.toList();

		int from = (int) Math.min(pageable.getOffset(), matching.size());
		int to = Math.min(from + pageable.getPageSize(), matching.size());

		return new PageImpl<>(matching.subList(from, to), pageable, matching.size());
	}

	public static Page<EventResponseDto> createMyEventResponsePage() {
		return createMyEventResponsePage(MyEventsFilter.ALL, createDefaultPageable());
	}

	public static Page<EventResponseDto> createMyEventResponsePage(MyEventsFilter filter, Pageable pageable) {
		return createMyEventPage(filter, pageable).map(EventTestDataFactory::toEventResponseDto);
	}

	// Same as EventService#count: no sort, because eq(...) compares the sort too.
	public static Pageable createCountPageable() {
		return PageRequest.of(0, 1);
	}

	public static CountsOfEventsDto createCountsOfEventsDto() {
		return CountsOfEventsDto.builder()
				.countOfAllEvents(4)
				.countOfDraftEvents(1)
				.countOfPublishedEvents(1)
				.countOfCanceledEvents(1)
				.countOfFinishedEvents(1)
				.build();
	}

	// ---------------------------------------------------------------------------
	// Create payloads
	// ---------------------------------------------------------------------------

	public static CreateEventDto createCreateEventDto() {
		return createCreateEventDto(DEFAULT_CATEGORY_ID);
	}

	public static CreateEventDto createCreateEventDto(UUID categoryId) {
		return new CreateEventDto(
				DEFAULT_TITLE,
				DEFAULT_DESCRIPTION,
				categoryId,
				DEFAULT_VENUE,
				DEFAULT_CITY,
				DEFAULT_STARTS_AT,
				DEFAULT_SALE_STARTS_AT,
				DEFAULT_IMAGE_URL);
	}

	public static List<CreateEventDto> createCreateEventDtoList(UUID categoryId) {
		return createEventList().stream()
				.map(event -> new CreateEventDto(
						event.getTitle(),
						event.getDescription(),
						categoryId,
						event.getVenue(),
						event.getCity(),
						event.getStartsAt(),
						event.getSaleStartsAt(),
						event.getImageUrl()))
				.toList();
	}

	public static CreateEventDto createCreatePastEventDto() {
		return new CreateEventDto(
				DEFAULT_TITLE,
				DEFAULT_DESCRIPTION,
				DEFAULT_CATEGORY_ID,
				DEFAULT_VENUE,
				DEFAULT_CITY,
				PAST_STARTS_AT,
				DEFAULT_SALE_STARTS_AT,
				DEFAULT_IMAGE_URL);
	}

	public static CreateEventDto createInvalidCreateEventDto() {
		return new CreateEventDto(
				"",
				"",
				null,
				"",
				"",
				null,
				null,
				"not-a-url");
	}

	// ---------------------------------------------------------------------------
	// Update payloads
	// ---------------------------------------------------------------------------

	public static UpdateEventDto createUpdateEventDto() {
		return createUpdateEventDto(DEFAULT_CATEGORY_ID);
	}

	public static UpdateEventDto createUpdateEventDto(UUID categoryId) {
		return updateEventDto(categoryId, UPDATED_STARTS_AT, UPDATED_SALE_STARTS_AT, Status.DRAFT, DEFAULT_VERSION);
	}

	public static UpdateEventDto createUpdateEventDtoWithStatus(Status status) {
		return updateEventDto(DEFAULT_CATEGORY_ID, UPDATED_STARTS_AT, UPDATED_SALE_STARTS_AT, status, DEFAULT_VERSION);
	}

	public static UpdateEventDto createUpdateEventDtoWithUnchangedStartsAt() {
		return updateEventDto(DEFAULT_CATEGORY_ID, DEFAULT_STARTS_AT, DEFAULT_SALE_STARTS_AT, Status.DRAFT,
				DEFAULT_VERSION);
	}

	public static UpdateEventDto createUpdateEventDtoWithPastStartsAt() {
		return updateEventDto(DEFAULT_CATEGORY_ID, PAST_STARTS_AT, PAST_SALE_STARTS_AT, Status.DRAFT, DEFAULT_VERSION);
	}

	public static UpdateEventDto createUpdateEventDtoWithSaleAtStart() {
		return updateEventDto(DEFAULT_CATEGORY_ID, UPDATED_STARTS_AT, UPDATED_STARTS_AT, Status.DRAFT, DEFAULT_VERSION);
	}

	public static UpdateEventDto createUpdateEventDtoWithSaleAfterStart() {
		return updateEventDto(DEFAULT_CATEGORY_ID, UPDATED_STARTS_AT, UPDATED_STARTS_AT.plusSeconds(14 * 24 * 60 * 60),
				Status.DRAFT, DEFAULT_VERSION);
	}

	public static UpdateEventDto createInvalidUpdateEventDto() {
		return new UpdateEventDto(
				"",
				"",
				null,
				"",
				"",
				null,
				null,
				"not-a-url",
				null,
				null);
	}

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	private static UpdateEventDto updateEventDto(UUID categoryId, Instant startsAt, Instant saleStartsAt,
			Status status, Long version) {
		return new UpdateEventDto(
				UPDATED_TITLE,
				UPDATED_DESCRIPTION,
				categoryId,
				UPDATED_VENUE,
				UPDATED_CITY,
				startsAt,
				saleStartsAt,
				UPDATED_IMAGE_URL,
				status,
				version);
	}

	// Mapped by hand on purpose: with the real EventMapper a mapper bug would end
	// up in the expected values too, and the tests would still pass.
	private static EventResponseDto toEventResponseDto(Event event) {
		Category category = event.getCategory();
		return EventResponseDto.builder()
				.id(event.getId())
				.version(event.getVersion())
				.category(new CategoryResponseDto(category.getId(), category.getTitle()))
				.title(event.getTitle())
				.description(event.getDescription())
				.venue(event.getVenue())
				.city(event.getCity())
				.startsAt(event.getStartsAt())
				.saleStartsAt(event.getSaleStartsAt())
				.imageUrl(event.getImageUrl())
				.status(event.getStatus())
				.createdAt(event.getCreatedAt())
				.updatedAt(event.getUpdatedAt())
				.build();
	}
}
