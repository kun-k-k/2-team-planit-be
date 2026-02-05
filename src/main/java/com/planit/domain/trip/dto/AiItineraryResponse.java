package com.planit.domain.trip.dto;

import java.util.List;

// AI 응답 바디를 매핑하기 위한 DTO
public record AiItineraryResponse(
        String message,
        Long tripId,
        List<AiItineraryDayResponse> itineraries
) {
}
