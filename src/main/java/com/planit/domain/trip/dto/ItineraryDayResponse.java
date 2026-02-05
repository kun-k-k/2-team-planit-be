package com.planit.domain.trip.dto;

import java.time.LocalDate;
import java.util.List;

public record ItineraryDayResponse(
        Long itineraryDayId,
        int day,
        LocalDate date,
        List<ItineraryActivityResponse> activities
) {
}
