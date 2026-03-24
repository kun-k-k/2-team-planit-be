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

import com.planit.domain.trip.service.AiAccessor.AiItineraryClient;
import com.planit.domain.trip.service.AiAccessor.AiItineraryJob;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiItineraryProcessor {
    private static final Logger log = LoggerFactory.getLogger(AiItineraryProcessor.class);

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
        log.info("[AI_PROCESS] start job tripId={}", job.request().tripId());
        AiItineraryResponse response = client.requestItinerary(job.request());
        saveItinerary(job.request().tripId(), response);
        log.info("[AI_PROCESS] end job tripId={}", job.request().tripId());
    }

    @Transactional
    public void processResponse(AiItineraryResponse response) {
        if (response == null) {
            return;
        }
        log.info("[AI_PROCESS] processResponse tripId={}", response.tripId());
        saveItinerary(response.tripId(), response);
    }

    private void saveItinerary(Long tripId, AiItineraryResponse response) {
        if (tripId == null || response == null || response.itineraries() == null) {
            return;
        }

        // 여행이 존재할 때만 일정 저장
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            log.warn("[AI_PROCESS] trip not found tripId={}", tripId);
            return;
        }

        for (AiItineraryDayResponse itinerary : response.itineraries()) {

            // 일자별 일정 저장
            ItineraryDay day = itineraryDayRepository.save(new ItineraryDay(
                    trip,
                    itinerary.day(),
                    itinerary.date() != null ? itinerary.date().atStartOfDay() : null
            ));
            log.info("[AI_PROCESS] day saved dayId={}, dayIndex={}", day.getId(), day.getDayIndex());


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
                    log.debug("[AI_PROCESS] transport saved dayId={}, order={}", day.getId(), activity.eventOrder());
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
                        activity.googleMapUrl(),
                        activity.googlePlaceId()
                    ));
                    log.debug("[AI_PROCESS] place saved dayId={}, name={}", day.getId(), activity.placeName());
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
