package com.ridehailing.ride.repository;

import com.ridehailing.ride.domain.Ride;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, UUID> {
}
