package com.planit.domain.trip.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ItineraryActivityResponse(
        Long activityId,
        String placeName,
        String transport,
        String type,
        Integer eventOrder,
        LocalTime startTime,
        Integer duration,
        BigDecimal cost,
        String memo,
        String googleMapUrl
) {
}
