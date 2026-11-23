package com.overex.backend.event;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import com.overex.backend.event.dto.responses.StatusOption;

/**
 * The one place that decides which status changes an event may make.
 *
 * <p>
 * Used twice with the same rules:
 * <ul>
 * <li>{@code GET /events/{id}} calls {@link #statusOptions} so the frontend
 * can grey out the options that are not allowed (and show why).</li>
 * <li>{@code PATCH /events/{id}} calls {@link #check} for the requested
 * status and rejects the update with 400 if it is blocked.</li>
 * </ul>
 *
 * <p>
 * All rules at a glance (from = stored status, to = requested status):
 *
 * <pre>
 * from \ to   DRAFT                       PUBLISHED                 CANCELED
 * DRAFT       allowed                     allowed until the start   never (delete instead)
 * PUBLISHED   allowed until sale opens    allowed                   allowed until the start
 * CANCELED    never                       never                     allowed
 * </pre>
 *
 * Staying in the same status is always allowed, so a normal edit (title,
 * venue, …) that keeps the status never fails here.
 */
public final class EventStatusRules {

    private EventStatusRules() {
    }

    /**
     * Whether the event may change from its stored status to {@code to}.
     *
     * <p>
     * Example: today is Aug 1, the event is PUBLISHED, its sale opened on
     * Jul 15 and it starts on Aug 12. The client asks for {@code to = DRAFT}.
     *
     * <pre>
     * 1. eventStartedAlready     = !(Aug 12 is after Aug 1) = !true  = false
     *    eventSaleStartedAlready = !(Jul 15 is after Aug 1) = !false = true
     * 2. outer switch on the stored status → case PUBLISHED
     * 3. inner switch on the requested status → case DRAFT
     * 4. eventSaleStartedAlready is true
     *    → blocked("The ticket sale has already started, so the event can't go back to draft.")
     *    → the PATCH answers 400 with this text on the field "status"
     * </pre>
     *
     * Same event, but {@code to = CANCELED}: step 3 takes case CANCELED,
     * eventStartedAlready is false → allowed(), the event may be canceled.
     *
     * <p>
     * "Already started" uses {@code !isAfter(now)}, so the exact moment of the
     * start counts as started (start = now → blocked).
     *
     * <p>
     * The switches cover every enum value without a {@code default}, so the
     * compiler reports a missing case if a new {@link Status} is added.
     *
     * @param event the stored event (its status is the "from" side)
     * @param to    the status the client asks for
     * @param now   the current time, passed in so tests can use a fixed one
     * @return {@link StatusOption#allowed()} or
     *         {@link StatusOption#blocked(String)} with the reason
     */
    public static StatusOption check(Event event, Status to, Instant now) {

        boolean eventStartedAlready = !event.getStartsAt().isAfter(now);
        boolean eventSaleStartedAlready = !event.getSaleStartsAt().isAfter(now);

        return switch (event.getStatus()) {
            case DRAFT -> switch (to) {
                case DRAFT -> StatusOption.allowed();
                case PUBLISHED -> eventStartedAlready
                        ? StatusOption.blocked("This event has already started, so it can't be published anymore.")
                        : StatusOption.allowed();
                case CANCELED -> StatusOption.blocked(
                        "A draft was never public, so there is nothing to cancel. Delete it instead.");
            };
            case PUBLISHED -> switch (to) {
                case PUBLISHED -> StatusOption.allowed();
                case DRAFT -> eventSaleStartedAlready
                        ? StatusOption
                                .blocked("The ticket sale has already started, so the event can't go back to draft.")
                        : StatusOption.allowed();
                case CANCELED -> eventStartedAlready
                        ? StatusOption.blocked("This event has already started, so it can't be canceled anymore.")
                        : StatusOption.allowed();
            };
            case CANCELED -> switch (to) {
                case CANCELED -> StatusOption.allowed();
                case PUBLISHED -> StatusOption.blocked("A canceled event can't be published again.");
                case DRAFT -> StatusOption.blocked("A canceled event can't be turned back into a draft.");
            };
        };
    }

    /**
     * {@link #check} for every status at once, for {@code GET /events/{id}}.
     *
     * <p>
     * Example: a DRAFT that starts far in the future.
     *
     * <pre>
     * 1. loop to = DRAFT     → check → allowed           (staying is always fine)
     * 2. loop to = PUBLISHED → check → allowed           (not started yet)
     * 3. loop to = CANCELED  → check → blocked("A draft was never public, …")
     *
     * JSON in the response:
     * "statusOptions": {
     *   "DRAFT":     { "disabled": false, "why": null },
     *   "PUBLISHED": { "disabled": false, "why": null },
     *   "CANCELED":  { "disabled": true,  "why": "A draft was never public, …" }
     * }
     * </pre>
     *
     * An {@link EnumMap} keeps the keys in enum order (DRAFT, PUBLISHED,
     * CANCELED), so the select in the frontend always lists them the same way.
     */
    public static Map<Status, StatusOption> statusOptions(Event event, Instant now) {

        Map<Status, StatusOption> statusOptions = new EnumMap<>(Status.class);

        for (Status to : Status.values()) {
            statusOptions.put(to, check(event, to, now));
        }

        return statusOptions;

    }

}
