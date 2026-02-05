package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AiItineraryRequest(
        Long tripId,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate arrivalDate,
        @JsonFormat(pattern = "HH:mm")
        LocalTime arrivalTime,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate departureDate,
        @JsonFormat(pattern = "HH:mm")
        LocalTime departureTime,

        String travelCity,
        Integer totalBudget,

        List<String> travelTheme,
        List<String> wantedPlace
) {
}
