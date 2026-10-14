package com.overex.backend.event.utils;

import java.time.Instant;
import java.util.UUID;

import com.overex.backend.event.Event;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;

/**
 * Reusable event fixtures.
 *
 * <p>All timestamps are fixed and lie far in the future, so payloads always pass
 * the {@code @Future} check on {@link CreateEventDto#startsAt()}.
 */
public final class EventTestDataFactory {

	private EventTestDataFactory() {
	}

	/** A fixed id so tests can assert against path variables and responses. */
	public static final UUID DEFAULT_EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	public static final Instant DEFAULT_STARTS_AT = Instant.parse("2099-06-20T19:00:00Z");
	public static final Instant DEFAULT_SALE_STARTS_AT = Instant.parse("2099-03-01T10:00:00Z");
	public static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-01T12:00:00Z");

	/** A persisted-looking event (with id and timestamps) in {@code DRAFT} status. */
	public static Event createDefaultEvent() {
		return Event.builder()
				.id(DEFAULT_EVENT_ID)
				.title("Summer Festival")
				.description("Open-air festival with live bands.")
				.venue("Donauinsel")
				.city("Wien")
				.startsAt(DEFAULT_STARTS_AT)
				.saleStartsAt(DEFAULT_SALE_STARTS_AT)
				.imageUrl("https://example.com/images/summer-festival.png")
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

	/** A valid create payload matching {@link #createDefaultEvent()}. */
	public static CreateEventDto createCreateEventDto() {
		return new CreateEventDto(
				"Summer Festival",
				"Open-air festival with live bands.",
				"Donauinsel",
				"Wien",
				DEFAULT_STARTS_AT,
				DEFAULT_SALE_STARTS_AT,
				"https://example.com/images/summer-festival.png");
	}

	/** A valid update payload that changes every field of {@link #createDefaultEvent()}. */
	public static UpdateEventDto createUpdateEventDto() {
		return new UpdateEventDto(
				"Winter Festival",
				"Indoor festival with DJs.",
				"Stadthalle",
				"Graz",
				Instant.parse("2099-12-10T20:00:00Z"),
				Instant.parse("2099-09-01T10:00:00Z"),
				"https://example.com/images/winter-festival.png");
	}

	/** {@link #createDefaultEvent()} after {@link #createUpdateEventDto()} was applied. */
	public static Event createUpdatedEvent() {
		Event event = createDefaultEvent();
		event.setTitle("Winter Festival");
		event.setDescription("Indoor festival with DJs.");
		event.setVenue("Stadthalle");
		event.setCity("Graz");
		event.setStartsAt(Instant.parse("2099-12-10T20:00:00Z"));
		event.setSaleStartsAt(Instant.parse("2099-09-01T10:00:00Z"));
		event.setImageUrl("https://example.com/images/winter-festival.png");
		return event;
	}

	/** Same as {@link #createDefaultEvent()} but its start lies in the past, so it can't be updated anymore. */
	public static Event createStartedEvent() {
		Event event = createDefaultEvent();
		event.setStartsAt(Instant.parse("2020-06-20T19:00:00Z"));
		event.setSaleStartsAt(Instant.parse("2020-03-01T10:00:00Z"));
		return event;
	}

	/** Keeps the start time of {@link #createDefaultEvent()}, so the "moved to the past" check is skipped. */
	public static UpdateEventDto createUpdateEventDtoWithUnchangedStartsAt() {
		return new UpdateEventDto(
				"Winter Festival",
				"Indoor festival with DJs.",
				"Stadthalle",
				"Graz",
				DEFAULT_STARTS_AT,
				DEFAULT_SALE_STARTS_AT,
				"https://example.com/images/winter-festival.png");
	}

	/** Moves the start time into the past. */
	public static UpdateEventDto createUpdateEventDtoWithPastStartsAt() {
		return new UpdateEventDto(
				"Winter Festival",
				"Indoor festival with DJs.",
				"Stadthalle",
				"Graz",
				Instant.parse("2020-12-10T20:00:00Z"),
				Instant.parse("2020-09-01T10:00:00Z"),
				"https://example.com/images/winter-festival.png");
	}

	/** The sale opens exactly when the event starts (boundary: must be rejected). */
	public static UpdateEventDto createUpdateEventDtoWithSaleAtStart() {
		return new UpdateEventDto(
				"Winter Festival",
				"Indoor festival with DJs.",
				"Stadthalle",
				"Graz",
				Instant.parse("2099-12-10T20:00:00Z"),
				Instant.parse("2099-12-10T20:00:00Z"),
				"https://example.com/images/winter-festival.png");
	}

	/** The sale opens after the event starts. */
	public static UpdateEventDto createUpdateEventDtoWithSaleAfterStart() {
		return new UpdateEventDto(
				"Winter Festival",
				"Indoor festival with DJs.",
				"Stadthalle",
				"Graz",
				Instant.parse("2099-12-10T20:00:00Z"),
				Instant.parse("2099-12-24T10:00:00Z"),
				"https://example.com/images/winter-festival.png");
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
}
