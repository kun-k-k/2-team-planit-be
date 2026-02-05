package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.ItineraryItemTransport;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryItemTransportRepository extends JpaRepository<ItineraryItemTransport, Long> {
    List<ItineraryItemTransport> findByItineraryDayIdOrderByEventOrder(Long itineraryDayId);
    void deleteByItineraryDayIdIn(Collection<Long> itineraryDayIds);
}
