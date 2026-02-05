package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.WantedPlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WantedPlaceRepository extends JpaRepository<WantedPlace, Long> {
    void deleteByTripId(Long tripId);
    List<WantedPlace> findByTripId(Long tripId);
}
