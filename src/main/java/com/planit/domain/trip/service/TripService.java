package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.entity.WantedPlace;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.repository.TripThemeRepository;
import com.planit.domain.trip.repository.WantedPlaceRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TripThemeRepository tripThemeRepository;
    private final WantedPlaceRepository wantedPlaceRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemPlaceRepository itineraryItemPlaceRepository;
    private final ItineraryItemTransportRepository itineraryItemTransportRepository;
    private final AiItineraryQueue aiItineraryQueue;
    private final AiItineraryProcessor aiItineraryProcessor;
    private final boolean aiMockEnabled;

    public TripService(
            TripRepository tripRepository,
            TripThemeRepository tripThemeRepository,
            WantedPlaceRepository wantedPlaceRepository,
            ItineraryDayRepository itineraryDayRepository,
            ItineraryItemPlaceRepository itineraryItemPlaceRepository,
            ItineraryItemTransportRepository itineraryItemTransportRepository,
            AiItineraryQueue aiItineraryQueue,
            AiItineraryProcessor aiItineraryProcessor,
            @Value("${ai.mock-enabled:false}") boolean aiMockEnabled
    ) {
        this.tripRepository = tripRepository;
        this.tripThemeRepository = tripThemeRepository;
        this.wantedPlaceRepository = wantedPlaceRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryItemPlaceRepository = itineraryItemPlaceRepository;
        this.itineraryItemTransportRepository = itineraryItemTransportRepository;
        this.aiItineraryQueue = aiItineraryQueue;
        this.aiItineraryProcessor = aiItineraryProcessor;
        this.aiMockEnabled = aiMockEnabled;
    }

    @Transactional
    public Long createTrip(TripCreateRequest request) {
        Trip trip = tripRepository.save(new Trip(
                request.title(),
                request.arrivalDate(),
                request.departureDate(),
                request.arrivalTime(),
                request.departureTime(),
                request.travelCity(),
                request.totalBudget()
        ));
        System.out.println("여행 저장 후 tripId 반환: " + trip.getId());

        // 테마 저장 (Trip 1 : Theme N)
        for (String theme : request.travelTheme()) {
            trip.addTheme(new TripTheme(trip, theme));
        }
        tripRepository.save(trip);

        // 희망 장소 저장
        if (request.wantedPlace() != null) {
            for (String placeId : request.wantedPlace()) {
                wantedPlaceRepository.save(new WantedPlace(trip, placeId));
            }
        }

        // AI 요청을 큐에 적재 (mock 모드면 즉시 처리)
        AiItineraryJob job = new AiItineraryJob(new AiItineraryRequest(
                trip.getId(),
                trip.getArrivalDate(),
                trip.getArrivalTime(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getTravelCity(),
                trip.getTotalBudget(),
                request.travelTheme(),
                request.wantedPlace()
        ));
        if (aiMockEnabled) {
            System.out.println("AI mock 모드: 즉시 일정 생성 처리");
            aiItineraryProcessor.process(job);
        } else {
            aiItineraryQueue.enqueue(job);
        }

        return trip.getId();
    }


}
