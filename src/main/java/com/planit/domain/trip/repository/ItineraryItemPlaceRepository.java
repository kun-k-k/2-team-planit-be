package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.ItineraryItemPlace;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryItemPlaceRepository extends JpaRepository<ItineraryItemPlace, Long> {
    List<ItineraryItemPlace> findByItineraryDayIdOrderByEventOrder(Long itineraryDayId);
    void deleteByItineraryDayIdIn(Collection<Long> itineraryDayIds);
}
