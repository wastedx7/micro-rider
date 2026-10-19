package com.ridehailing.ride.service;

import com.ridehailing.proto.ride.CancelRideRequest;
import com.ridehailing.proto.ride.CreateRideRequest;
import com.ridehailing.proto.ride.GetRideRequest;
import com.ridehailing.proto.ride.GetRideStatusRequest;
import com.ridehailing.proto.ride.RideResponse;
import com.ridehailing.proto.ride.RideServiceGrpc;
import com.ridehailing.proto.ride.RideStatusResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class RideGrpcService extends RideServiceGrpc.RideServiceImplBase {
    private final RideApplicationService rides;

    public RideGrpcService(RideApplicationService rides) { this.rides = rides; }

    @Override
    public void createRide(CreateRideRequest request, StreamObserver<RideResponse> observer) {
        try {
            UUID riderId = UUID.fromString(request.getRiderId());
            var ride = rides.create(riderId, request.getPickupLocation(), request.getDropoffLocation());
            observer.onNext(response(ride));
            observer.onCompleted();
        } catch (IllegalArgumentException ex) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription("Invalid rider or location").asRuntimeException());
        } catch (Exception ex) {
            observer.onError(Status.INTERNAL.withDescription("Ride creation failed").asRuntimeException());
        }
    }

    @Override
    public void getRide(GetRideRequest request, StreamObserver<RideResponse> observer) {
        try {
            observer.onNext(response(rides.get(UUID.fromString(request.getRideId()))));
            observer.onCompleted();
        } catch (RideApplicationService.RideNotFoundException ex) {
            observer.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException ex) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription("Invalid ride ID").asRuntimeException());
        }
    }

    @Override
    public void cancelRide(CancelRideRequest request, StreamObserver<RideResponse> observer) {
        try {
            observer.onNext(response(rides.cancel(UUID.fromString(request.getRideId()))));
            observer.onCompleted();
        } catch (IllegalStateException ex) {
            observer.onError(Status.FAILED_PRECONDITION.withDescription(ex.getMessage()).asRuntimeException());
        } catch (RideApplicationService.RideNotFoundException ex) {
            observer.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getRideStatus(GetRideStatusRequest request, StreamObserver<RideStatusResponse> observer) {
        try {
            var ride = rides.get(UUID.fromString(request.getRideId()));
            observer.onNext(RideStatusResponse.newBuilder().setRideId(ride.getId().toString())
                    .setDriverId(ride.getDriverId() == null ? "" : ride.getDriverId().toString())
                    .setStatus(com.ridehailing.proto.ride.RideStatus.valueOf(ride.getStatus().name()))
                    .setUpdatedAtEpochMs(ride.getUpdatedAt().toEpochMilli()).build());
            observer.onCompleted();
        } catch (Exception ex) {
            observer.onError(Status.NOT_FOUND.withDescription("Ride not found").asRuntimeException());
        }
    }

    private RideResponse response(com.ridehailing.ride.domain.Ride ride) {
        return RideResponse.newBuilder().setRideId(ride.getId().toString())
                .setRiderId(ride.getRiderId().toString())
                .setDriverId(ride.getDriverId() == null ? "" : ride.getDriverId().toString())
                .setStatus(com.ridehailing.proto.ride.RideStatus.valueOf(ride.getStatus().name()))
                .setCreatedAtEpochMs(ride.getCreatedAt().toEpochMilli())
                .setUpdatedAtEpochMs(ride.getUpdatedAt().toEpochMilli()).build();
    }
}
