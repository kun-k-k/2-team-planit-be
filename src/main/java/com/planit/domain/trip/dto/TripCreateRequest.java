package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TripCreateRequest(
        @NotBlank
        @Size(max = 15)
        String title,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate arrivalDate,
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime arrivalTime,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate departureDate,
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime departureTime,

        @NotBlank
        String travelCity,
        @NotNull
        @Positive
        Integer totalBudget,

        @NotEmpty
        List<@NotBlank String> travelTheme,
        List<@NotBlank String> wantedPlace
) {
}
