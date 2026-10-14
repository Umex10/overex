package com.overex.backend.event.utils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.overex.backend.event.Event;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;

/**
 * Reusable event fixtures.
 *
 * <p>Every value is defined once as a constant below and only referenced by the
 * factory methods, so an entity and the payload it is built from always match.
 * The future timestamps lie far ahead, so payloads always pass the
 * {@code @Future} check on {@link CreateEventDto#startsAt()}.
 */
public final class EventTestDataFactory {

	private EventTestDataFactory() {
	}

	/** A fixed id so tests can assert against path variables and responses. */
	public static final UUID DEFAULT_EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	// Values of the default event and the create payload.
	public static final String DEFAULT_TITLE = "Summer Festival";
	public static final String DEFAULT_DESCRIPTION = "Open-air festival with live bands.";
	public static final String DEFAULT_VENUE = "Donauinsel";
	public static final String DEFAULT_CITY = "Wien";
	public static final Instant DEFAULT_STARTS_AT = Instant.parse("2099-06-20T19:00:00Z");
	public static final Instant DEFAULT_SALE_STARTS_AT = Instant.parse("2099-03-01T10:00:00Z");
	public static final String DEFAULT_IMAGE_URL = "https://example.com/images/summer-festival.png";
	public static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-01T12:00:00Z");

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

	// Values of the second event of the list; every field differs from the default event.
	public static final UUID SECOND_EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	public static final String SECOND_TITLE = "Jazz Night";
	public static final String SECOND_DESCRIPTION = "An evening of live jazz.";
	public static final String SECOND_VENUE = "Porgy & Bess";
	public static final String SECOND_CITY = "Wien";
	public static final Instant SECOND_STARTS_AT = Instant.parse("2099-08-15T20:00:00Z");
	public static final Instant SECOND_SALE_STARTS_AT = Instant.parse("2099-05-01T09:00:00Z");
	public static final String SECOND_IMAGE_URL = "https://example.com/images/jazz-night.png";

	// Values of the third event of the list; every field differs from the other two.
	public static final UUID THIRD_EVENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	public static final String THIRD_TITLE = "Rock am Berg";
	public static final String THIRD_DESCRIPTION = "Two days of rock music in the mountains.";
	public static final String THIRD_VENUE = "Festivalgelände Mittersill";
	public static final String THIRD_CITY = "Salzburg";
	public static final Instant THIRD_STARTS_AT = Instant.parse("2099-07-04T16:00:00Z");
	public static final Instant THIRD_SALE_STARTS_AT = Instant.parse("2099-01-15T08:00:00Z");
	public static final String THIRD_IMAGE_URL = "https://example.com/images/rock-am-berg.png";

	/** A persisted-looking event (with id and timestamps) in {@code DRAFT} status. */
	public static Event createDefaultEvent() {
		return Event.builder()
				.id(DEFAULT_EVENT_ID)
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

	/** Same as {@link #createDefaultEvent()} but in {@code PUBLISHED} status. */
	public static Event createPublishedEvent() {
		Event event = createDefaultEvent();
		event.setStatus(Status.PUBLISHED);
		return event;
	}

	/** Same as {@link #createDefaultEvent()} but in {@code CANCELED} status. */
	public static Event createCanceledEvent() {
		Event event = createDefaultEvent();
		event.setStatus(Status.CANCELED);
		return event;
	}

	/**
	 * Three different events for list endpoints: {@link #createDefaultEvent()}
	 * ({@code DRAFT}), a {@code PUBLISHED} and a {@code CANCELED} one. Each call
	 * returns new instances, so tests can change them freely.
	 */
	public static List<Event> createEventList() {
		Event secondEvent = Event.builder()
				.id(SECOND_EVENT_ID)
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

	/** {@link #createDefaultEvent()} after {@link #createUpdateEventDto()} was applied. */
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

	/** Same as {@link #createDefaultEvent()} but its start lies in the past, so it can't be updated anymore. */
	public static Event createStartedEvent() {
		Event event = createDefaultEvent();
		event.setStartsAt(PAST_STARTS_AT);
		event.setSaleStartsAt(PAST_SALE_STARTS_AT);
		return event;
	}

	/** A valid create payload matching {@link #createDefaultEvent()}. */
	public static CreateEventDto createCreateEventDto() {
		return new CreateEventDto(
				DEFAULT_TITLE,
				DEFAULT_DESCRIPTION,
				DEFAULT_VENUE,
				DEFAULT_CITY,
				DEFAULT_STARTS_AT,
				DEFAULT_SALE_STARTS_AT,
				DEFAULT_IMAGE_URL);
	}

	/** An invalid create payload matching {@link #createDefaultEvent()}. */
	public static CreateEventDto createCreatePastEventDto() {
		return new CreateEventDto(
				DEFAULT_TITLE,
				DEFAULT_DESCRIPTION,
				DEFAULT_VENUE,
				DEFAULT_CITY,
				PAST_STARTS_AT,
				DEFAULT_SALE_STARTS_AT,
				DEFAULT_IMAGE_URL);
	}

	/** A valid update payload that changes every field of {@link #createDefaultEvent()}. */
	public static UpdateEventDto createUpdateEventDto() {
		return updateEventDto(UPDATED_STARTS_AT, UPDATED_SALE_STARTS_AT);
	}

	/** Keeps the start time of {@link #createDefaultEvent()}, so the "moved to the past" check is skipped. */
	public static UpdateEventDto createUpdateEventDtoWithUnchangedStartsAt() {
		return updateEventDto(DEFAULT_STARTS_AT, DEFAULT_SALE_STARTS_AT);
	}

	/** Moves the start time into the past. */
	public static UpdateEventDto createUpdateEventDtoWithPastStartsAt() {
		return updateEventDto(PAST_STARTS_AT, PAST_SALE_STARTS_AT);
	}

	/** The sale opens exactly when the event starts (boundary: must be rejected). */
	public static UpdateEventDto createUpdateEventDtoWithSaleAtStart() {
		return updateEventDto(UPDATED_STARTS_AT, UPDATED_STARTS_AT);
	}

	/** The sale opens after the event starts. */
	public static UpdateEventDto createUpdateEventDtoWithSaleAfterStart() {
		return updateEventDto(UPDATED_STARTS_AT, UPDATED_STARTS_AT.plusSeconds(14 * 24 * 60 * 60));
	}

	/** Breaks every bean-validation rule of {@link UpdateEventDto}, so {@code @Valid} rejects it. */
	public static UpdateEventDto createInvalidUpdateEventDto() {
		return new UpdateEventDto(
				"",
				"",
				"",
				"",
				null,
				null,
				"not-a-url");
	}

	/**
	 * The update payload with the {@code UPDATED_} texts; only the two timestamps
	 * vary between the update variants above.
	 */
	private static UpdateEventDto updateEventDto(Instant startsAt, Instant saleStartsAt) {
		return new UpdateEventDto(
				UPDATED_TITLE,
				UPDATED_DESCRIPTION,
				UPDATED_VENUE,
				UPDATED_CITY,
				startsAt,
				saleStartsAt,
				UPDATED_IMAGE_URL);
	}
}
