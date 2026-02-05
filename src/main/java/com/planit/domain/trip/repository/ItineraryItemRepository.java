package com.planit.domain.trip.repository;

import com.planit.domain.trip.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {
}
