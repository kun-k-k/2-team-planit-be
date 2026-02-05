package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

public record ItineraryDto(
        int day,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        List<ActivityDto> activities
) {
}
