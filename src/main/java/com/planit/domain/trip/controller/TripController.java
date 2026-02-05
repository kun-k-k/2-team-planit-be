package com.planit.domain.trip.controller;

import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.dto.TripCreateResponse;
import com.planit.domain.trip.service.TripService;
import com.planit.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<TripCreateResponse>> createTrip(@Valid @RequestBody TripCreateRequest request) {
        // 요청 검증 후 여행 생성 및 AI 큐 적재
        Long tripId = tripService.createTrip(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new TripCreateResponse(tripId)));
    }
}
