package com.ridehailing.common.events;

/** Stable event type names used in EventEnvelope metadata. */
public final class EventTypes {
    private EventTypes() {
    }

    public static final String RIDE_REQUESTED = "ride.requested";
    public static final String RIDE_STATUS_CHANGED = "ride.status_changed";
    public static final String DRIVER_LOCATION_UPDATED = "driver.location_updated";
    public static final String DRIVER_ASSIGNMENT_REQUESTED = "driver.assignment_requested";
    public static final String DRIVER_MATCH_NOTIFIED = "driver.match_notified";
    public static final String SESSION_READY = "session.ready";
    public static final String SESSION_CLOSED = "session.closed";
}
