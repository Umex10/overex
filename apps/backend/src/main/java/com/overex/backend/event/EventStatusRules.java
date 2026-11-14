package com.overex.backend.event;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import com.overex.backend.event.dto.responses.StatusOption;

public final class EventStatusRules {

    private EventStatusRules() {
    }

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

    public static Map<Status, StatusOption> statusOptions(Event event, Instant now) {

        Map<Status, StatusOption> statusOptions = new EnumMap<>(Status.class);

        for (Status to : Status.values()) {
            statusOptions.put(to, check(event, to, now));
        }

        return statusOptions;

    }

}
