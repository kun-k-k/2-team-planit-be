package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

// AI 응답의 activities[] 요소
public record AiItineraryActivityResponse(
        String placeName,
        String transport,
        String type,
        Integer eventOrder,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        Integer duration,
        Integer cost,
        String memo,
        String googleMapUrl
) {
}
