package com.ridehailing.ride.api;

import com.ridehailing.ride.domain.Ride;
import com.ridehailing.ride.service.RideApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rides")
public class RideController {
    private final RideApplicationService rides;

    public RideController(RideApplicationService rides) { this.rides = rides; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RideView create(@RequestHeader("X-User-ID") String userId,
                           @Valid @RequestBody CreateRideRequest request) {
        Ride ride = rides.create(UUID.fromString(userId), request.pickup().toProto(), request.dropoff().toProto());
        return RideView.from(ride);
    }

    @GetMapping("/{rideId}")
    public RideView get(@RequestHeader("X-User-ID") String userId,
                        @PathVariable UUID rideId) {
        return RideView.from(rides.getForRider(rideId, UUID.fromString(userId)));
    }

    @DeleteMapping("/{rideId}")
    public RideView cancel(@RequestHeader("X-User-ID") String userId,
                           @PathVariable UUID rideId) {
        return RideView.from(rides.cancelForRider(rideId, UUID.fromString(userId)));
    }

    public record CreateRideRequest(@NotNull LocationRequest pickup,
                                    @NotNull LocationRequest dropoff) {
    }

    public record LocationRequest(double latitude, double longitude, String address) {
        public com.ridehailing.proto.ride.Location toProto() {
            return com.ridehailing.proto.ride.Location.newBuilder()
                    .setLatitude(latitude).setLongitude(longitude)
                    .setAddress(address == null ? "" : address).build();
        }
    }

    public record RideView(String rideId, String riderId, String driverId,
                           String status, long createdAtEpochMs, long updatedAtEpochMs) {
        static RideView from(Ride ride) {
            return new RideView(ride.getId().toString(), ride.getRiderId().toString(),
                    ride.getDriverId() == null ? null : ride.getDriverId().toString(),
                    ride.getStatus().name(), ride.getCreatedAt().toEpochMilli(),
                    ride.getUpdatedAt().toEpochMilli());
        }
    }
}
