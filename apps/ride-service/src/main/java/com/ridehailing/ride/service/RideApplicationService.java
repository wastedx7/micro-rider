package com.ridehailing.ride.service;

import com.google.protobuf.MessageLite;
import com.ridehailing.proto.ride.Location;
import com.ridehailing.proto.ride.RideRequestedEvent;
import com.ridehailing.proto.ride.RideStatusChangeEvent;
import com.ridehailing.ride.domain.Ride;
import com.ridehailing.ride.domain.RideStatus;
import com.ridehailing.ride.repository.RideRepository;
import com.ridehailing.common.topics.KafkaTopics;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideApplicationService {
    private final RideRepository repository;
    private final KafkaTemplate<String, MessageLite> kafka;

    public RideApplicationService(RideRepository repository, KafkaTemplate<String, MessageLite> kafka) {
        this.repository = repository;
        this.kafka = kafka;
    }

    @Transactional
    public Ride create(UUID riderId, Location pickup, Location dropoff) {
        Ride ride = repository.save(new Ride(riderId, pickup.getLatitude(), pickup.getLongitude(),
                pickup.getAddress(), dropoff.getLatitude(), dropoff.getLongitude(), dropoff.getAddress()));
        publishRequested(ride);
        return ride;
    }

    @Transactional(readOnly = true)
    public Ride get(UUID rideId) {
        return repository.findById(rideId).orElseThrow(() -> new RideNotFoundException(rideId));
    }

    @Transactional(readOnly = true)
    public Ride getForRider(UUID rideId, UUID riderId) {
        Ride ride = get(rideId);
        if (!ride.getRiderId().equals(riderId)) {
            throw new RideAccessDeniedException();
        }
        return ride;
    }

    @Transactional
    public Ride cancel(UUID rideId) {
        Ride ride = get(rideId);
        ride.transitionTo(RideStatus.CANCELLED);
        Ride saved = repository.save(ride);
        publishStatus(saved);
        return saved;
    }

    @Transactional
    public Ride cancelForRider(UUID rideId, UUID riderId) {
        Ride ride = getForRider(rideId, riderId);
        ride.transitionTo(RideStatus.CANCELLED);
        Ride saved = repository.save(ride);
        publishStatus(saved);
        return saved;
    }

    @Transactional
    public Ride moveToMatching(UUID rideId) {
        Ride ride = get(rideId);
        ride.transitionTo(RideStatus.MATCHING);
        Ride saved = repository.save(ride);
        publishStatus(saved);
        return saved;
    }

    private void publishRequested(Ride ride) {
        RideRequestedEvent event = RideRequestedEvent.newBuilder()
                .setRideId(ride.getId().toString())
                .setRiderId(ride.getRiderId().toString())
                .setPickupLocation(location(ride.getPickupLatitude(), ride.getPickupLongitude(), ride.getPickupAddress()))
                .setDropoffLocation(location(ride.getDropoffLatitude(), ride.getDropoffLongitude(), ride.getDropoffAddress()))
                .setTimestampEpochMs(ride.getCreatedAt().toEpochMilli())
                .build();
        kafka.send(KafkaTopics.RIDE_STATUS, ride.getId().toString(), event);
    }

    private void publishStatus(Ride ride) {
        RideStatusChangeEvent event = RideStatusChangeEvent.newBuilder()
                .setRideId(ride.getId().toString())
                .setRiderId(ride.getRiderId().toString())
                .setStatus(com.ridehailing.proto.ride.RideStatus.valueOf(ride.getStatus().name()))
                .setUpdatedAtEpochMs(ride.getUpdatedAt().toEpochMilli())
                .build();
        kafka.send(KafkaTopics.RIDE_STATUS, ride.getId().toString(), event);
    }

    private Location location(double latitude, double longitude, String address) {
        return Location.newBuilder().setLatitude(latitude).setLongitude(longitude)
                .setAddress(address == null ? "" : address).build();
    }

    public static class RideNotFoundException extends RuntimeException {
        public RideNotFoundException(UUID rideId) { super("Ride not found: " + rideId); }
    }

    public static class RideAccessDeniedException extends RuntimeException {
        public RideAccessDeniedException() { super("Rider does not own this ride"); }
    }
}
