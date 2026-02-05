package com.planit.domain.trip.dto;

import java.util.List;

public record ItineraryResponse(
        Long tripId,
        List<ItineraryDayResponse> itineraries
) {
}
