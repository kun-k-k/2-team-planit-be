package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.ItineraryDay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
    List<ItineraryDay> findByTripIdOrderByDayIndex(Long tripId);
    List<ItineraryDay> findByTripId(Long tripId);
}
