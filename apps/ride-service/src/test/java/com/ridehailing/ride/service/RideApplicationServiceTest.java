package com.ridehailing.ride.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.MessageLite;
import com.ridehailing.proto.ride.Location;
import com.ridehailing.ride.domain.Ride;
import com.ridehailing.ride.repository.RideRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class RideApplicationServiceTest {
    private final RideRepository repository = mock(RideRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, MessageLite> kafka = mock(KafkaTemplate.class);
    private final RideApplicationService service = new RideApplicationService(repository, kafka);

    @Test
    void rejectsAccessByAnotherRider() {
        UUID owner = UUID.randomUUID();
        Ride ride = new Ride(owner, 1, 2, "pickup", 3, 4, "dropoff");
        when(repository.findById(ride.getId())).thenReturn(Optional.of(ride));

        assertThrows(RideApplicationService.RideAccessDeniedException.class,
                () -> service.getForRider(ride.getId(), UUID.randomUUID()));
    }

    @Test
    void publishesRideRequestedEventOnCreate() {
        UUID rider = UUID.randomUUID();
        when(repository.save(org.mockito.ArgumentMatchers.any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(rider, Location.newBuilder().setLatitude(1).setLongitude(2).build(),
                Location.newBuilder().setLatitude(3).setLongitude(4).build());

        verify(kafka).send(org.mockito.ArgumentMatchers.eq("ride-status"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(MessageLite.class));
    }
}
