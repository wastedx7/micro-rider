package com.ridehailing.common.topics;

/**
 * Logical names of all Kafka topics in the platform, mirroring the architecture
 * diagram ("Message queue", "location queue", "Driver Assignment queue").
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    /** Ride lifecycle events (producer: ride-service; consumer: ws-rider). */
    public static final String RIDE_STATUS = "ride-status";

    /** Driver location pings (producer: ws-driver; consumer: location-service). */
    public static final String DRIVER_LOCATION = "driver-location";

    /** Ride dispatch for matching (producer: ride-service; consumer: driver-assignment-service). */
    public static final String DRIVER_ASSIGNMENT = "driver-assignment";

    /** Client session readiness (producer: ws servers; emitted for observability). */
    public static final String SESSION_READY = "session-ready";

    public static final String[] ALL = {
            RIDE_STATUS,
            DRIVER_LOCATION,
            DRIVER_ASSIGNMENT,
            SESSION_READY
    };
}