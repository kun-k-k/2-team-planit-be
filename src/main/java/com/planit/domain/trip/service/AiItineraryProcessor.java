package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryActivityResponse;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.dto.AiItineraryDayResponse;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.entity.ItineraryItemTransport;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiItineraryProcessor {

    private final AiItineraryClient client;
    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemPlaceRepository placeRepository;
    private final ItineraryItemTransportRepository transportRepository;

    public AiItineraryProcessor(
            AiItineraryClient client,
            TripRepository tripRepository,
            ItineraryDayRepository itineraryDayRepository,
            ItineraryItemPlaceRepository placeRepository,
            ItineraryItemTransportRepository transportRepository
    ) {
        this.client = client;
        this.tripRepository = tripRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.placeRepository = placeRepository;
        this.transportRepository = transportRepository;
    }

    @Transactional
    public void process(AiItineraryJob job) {
        // AI 서버로 일정 생성 요청
        AiItineraryResponse response = client.requestItinerary(job.request());
        if (response == null || response.itineraries() == null) {
            return;
        }

        // 여행이 존재할 때만 일정 저장
        Trip trip = tripRepository.findById(job.request().tripId()).orElse(null);
        if (trip == null) {
            return;
        }

        for (AiItineraryDayResponse itinerary : response.itineraries()) {

            // 일자별 일정 저장
            ItineraryDay day = itineraryDayRepository.save(new ItineraryDay(
                    trip,
                    itinerary.day(),
                    itinerary.date() != null ? itinerary.date().atStartOfDay() : null
            ));
            System.out.println("일별일정 저장됨: "+day.getId()+"&"+day.getDayIndex());


            List<AiItineraryActivityResponse> activities = itinerary.activities();
            if (activities == null) {
                continue;
            }
            for (AiItineraryActivityResponse activity : activities) {
                if (activity == null || activity.type() == null) {
                    continue;
                }
                // 이동(route)은 이동 이벤트, 나머지는 장소 이벤트로 처리
                if (isRoute(activity.type())) {
                    transportRepository.save(new ItineraryItemTransport(
                        day,
                        activity.transport() != null ? activity.transport() : "UNKNOWN",
                        activity.type(),
                        activity.eventOrder() != null ? activity.eventOrder() : 0,
                        resolveStartTime(activity.startTime()),
                        resolveDuration(activity.duration())
                    ));
                } else {
                    placeRepository.save(new ItineraryItemPlace(
                        day,
                        null,
                        activity.placeName(),
                        activity.type(),
                        activity.eventOrder() != null ? activity.eventOrder() : 0,
                        resolveStartTime(activity.startTime()),
                        resolveDuration(activity.duration()),
                        resolveCost(activity.cost()),
                        activity.memo(),
                        activity.googleMapUrl()
                    ));
                }
            }
        }
    }

    private boolean isRoute(String type) {
        return "route".equalsIgnoreCase(type);
    }

    private LocalTime resolveStartTime(LocalTime startTime) {
        // 시작 시간이 없으면 00:00으로 보정
        return startTime != null ? startTime : LocalTime.MIDNIGHT;
    }

    private LocalTime resolveDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return LocalTime.MIDNIGHT;
        }
        // duration(분) -> LocalTime 변환
        return LocalTime.ofSecondOfDay(durationMinutes * 60L);
    }

    private BigDecimal resolveCost(Integer cost) {
        if (cost == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cost);
    }
}
