package com.overex.backend.event;

import java.time.Instant;

import lombok.Getter;

@Getter 
public class MyEventCriteria {

    private final Status status;
    private final Instant startsAfter;
    private final Instant startsBeforeOrAt;

    private MyEventCriteria(Status status, Instant startsAfter, Instant startsBeforeOrAt) {
        this.status = status;
        this.startsAfter = startsAfter;
        this.startsBeforeOrAt = startsBeforeOrAt;
    }

    public static MyEventCriteria of(MyEventsFilter filter, Instant now) {

        return switch (filter) {

            case DRAFT -> new MyEventCriteria(Status.DRAFT, null, null);
            case PUBLISHED -> new MyEventCriteria(Status.PUBLISHED, now, null);
            case CANCELED -> new MyEventCriteria(Status.CANCELED, null, null);
            case FINISHED -> new MyEventCriteria(Status.PUBLISHED, null, now);
            case ALL -> new MyEventCriteria(null,null, null);

        };
    }

}
