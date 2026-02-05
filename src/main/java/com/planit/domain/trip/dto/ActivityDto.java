package com.planit.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

// AI 응답의 activities[] 요소
public record ActivityDto(
        String type,
        String placeId,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        Integer cost,
        Integer duration,
        String memo
) {
}
