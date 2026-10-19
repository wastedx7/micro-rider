package com.ridehailing.ride.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RideTest {
    @Test
    void followsTheExpectedLifecycle() {
        Ride ride = new Ride(UUID.randomUUID(), 1.0, 2.0, "pickup",
                3.0, 4.0, "dropoff");

        ride.transitionTo(RideStatus.MATCHING);
        ride.transitionTo(RideStatus.DRIVER_ASSIGNED);
        ride.transitionTo(RideStatus.IN_PROGRESS);
        ride.transitionTo(RideStatus.COMPLETED);

        assertEquals(RideStatus.COMPLETED, ride.getStatus());
    }

    @Test
    void rejectsInvalidTransitions() {
        Ride ride = new Ride(UUID.randomUUID(), 1.0, 2.0, "pickup",
                3.0, 4.0, "dropoff");

        assertThrows(IllegalStateException.class,
                () -> ride.transitionTo(RideStatus.COMPLETED));
    }

    @Test
    void allowsCancellationBeforeCompletion() {
        Ride ride = new Ride(UUID.randomUUID(), 1.0, 2.0, "pickup",
                3.0, 4.0, "dropoff");

        ride.transitionTo(RideStatus.CANCELLED);

        assertEquals(RideStatus.CANCELLED, ride.getStatus());
        assertThrows(IllegalStateException.class,
                () -> ride.transitionTo(RideStatus.MATCHING));
    }
}
