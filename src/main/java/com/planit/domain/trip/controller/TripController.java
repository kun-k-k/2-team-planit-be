package com.planit.domain.trip.controller;

//import com.planit.domain.trip.dto.ItineraryRegenerateRequest;
import com.planit.domain.trip.dto.ItineraryResponse;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.dto.TripCreateResponse;
import com.planit.domain.trip.service.ItineraryQueryService;
import com.planit.domain.trip.service.TripService;
import com.planit.global.common.exception.ErrorCode;
import com.planit.global.common.response.ApiResponse;
import com.planit.global.common.response.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


// 깃 revert테스트용 커밋
@RestController
public class TripController {

    private final TripService tripService;
    private final ItineraryQueryService itineraryQueryService;

    public TripController(TripService tripService, ItineraryQueryService itineraryQueryService) {
        this.tripService = tripService;
        this.itineraryQueryService = itineraryQueryService;
    }

    @PostMapping("/trips")
    public ResponseEntity<ApiResponse<TripCreateResponse>> createTrip(@Valid @RequestBody TripCreateRequest request) {
        // 요청 검증 후 여행 생성 및 AI 큐 적재
        Long tripId = tripService.createTrip(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new TripCreateResponse(tripId)));
    }

    @GetMapping("/trips/{tripId}/itineraries")
    public ResponseEntity<?> getItineraries(@PathVariable Long tripId) {
        return itineraryQueryService.getTripItineraries(tripId)
                .<ResponseEntity<?>>map(response -> ResponseEntity.ok(ApiResponse.success(response)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.from(ErrorCode.TRIP_001)));
    }


}
