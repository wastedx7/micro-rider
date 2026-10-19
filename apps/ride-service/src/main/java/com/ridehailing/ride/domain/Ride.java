package com.ridehailing.ride.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class Ride {
    @Id
    private UUID id;
    @Column(name = "rider_id", nullable = false)
    private UUID riderId;
    @Column(name = "driver_id")
    private UUID driverId;
    @Column(name = "pickup_latitude", nullable = false)
    private double pickupLatitude;
    @Column(name = "pickup_longitude", nullable = false)
    private double pickupLongitude;
    @Column(name = "pickup_address")
    private String pickupAddress;
    @Column(name = "dropoff_latitude", nullable = false)
    private double dropoffLatitude;
    @Column(name = "dropoff_longitude", nullable = false)
    private double dropoffLongitude;
    @Column(name = "dropoff_address")
    private String dropoffAddress;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RideStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ride() { }

    public Ride(UUID riderId, double pickupLatitude, double pickupLongitude, String pickupAddress,
                double dropoffLatitude, double dropoffLongitude, String dropoffAddress) {
        this.id = UUID.randomUUID();
        this.riderId = riderId;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.pickupAddress = pickupAddress;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.dropoffAddress = dropoffAddress;
        this.status = RideStatus.REQUESTED;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getRiderId() { return riderId; }
    public UUID getDriverId() { return driverId; }
    public RideStatus getStatus() { return status; }
    public double getPickupLatitude() { return pickupLatitude; }
    public double getPickupLongitude() { return pickupLongitude; }
    public String getPickupAddress() { return pickupAddress; }
    public double getDropoffLatitude() { return dropoffLatitude; }
    public double getDropoffLongitude() { return dropoffLongitude; }
    public String getDropoffAddress() { return dropoffAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void assignDriver(UUID driverId) {
        transitionTo(RideStatus.DRIVER_ASSIGNED);
        this.driverId = driverId;
    }

    public void transitionTo(RideStatus next) {
        if (!isAllowed(status, next)) {
            throw new IllegalStateException("Invalid ride transition: " + status + " -> " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    private boolean isAllowed(RideStatus from, RideStatus to) {
        return switch (from) {
            case REQUESTED -> to == RideStatus.MATCHING || to == RideStatus.CANCELLED;
            case MATCHING -> to == RideStatus.DRIVER_ASSIGNED || to == RideStatus.CANCELLED;
            case DRIVER_ASSIGNED -> to == RideStatus.IN_PROGRESS || to == RideStatus.CANCELLED;
            case IN_PROGRESS -> to == RideStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
