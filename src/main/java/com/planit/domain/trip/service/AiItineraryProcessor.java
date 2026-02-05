package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.ActivityDto;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.dto.ItineraryDto;
import com.planit.domain.trip.entity.ItineraryItem;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.entity.ItineraryItemTransport;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.entity.Trip;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiItineraryProcessor {

    private final AiItineraryClient client;
    private final TripRepository tripRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final ItineraryItemPlaceRepository placeRepository;
    private final ItineraryItemTransportRepository transportRepository;

    public AiItineraryProcessor(
            AiItineraryClient client,
            TripRepository tripRepository,
            ItineraryItemRepository itineraryItemRepository,
            ItineraryItemPlaceRepository placeRepository,
            ItineraryItemTransportRepository transportRepository
    ) {
        this.client = client;
        this.tripRepository = tripRepository;
        this.itineraryItemRepository = itineraryItemRepository;
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

        // Persist only if trip exists
        Trip trip = tripRepository.findById(job.request().tripId()).orElse(null);
        if (trip == null) {
            return;
        }
        /*
        System.out.println("프로세서, 타이틀: "+trip.getTitle());
         */

        /*
        // DB 저장은 생략하고 로그로 흐름만 확인
        System.out.println("[DB 생략] tripId=" + job.request().tripId()
                + " 일정 " + response.itineraries().size() + "일치 저장 예정");
         */

        for (ItineraryDto itinerary : response.itineraries()) {

            // Create day item (1st day, 2nd day...)
            ItineraryItem item = itineraryItemRepository.save(new ItineraryItem(trip, itinerary.day()));

            // 일자별 일정 생성 로직 (DB 저장 생략)
            //System.out.println("[DB 생략] day=" + itinerary.day());

            List<ActivityDto> activities = itinerary.activities();
            if (activities == null) {
                continue;
            }
            int order = 1;
            for (ActivityDto activity : activities) {
                if (activity == null || activity.type() == null) {
                    order++;
                    continue;
                }
                // Route는 이동 이벤트, 나머지는 장소 이벤트로 처리
                if (isRoute(activity.type())) {
                    transportRepository.save(new ItineraryItemTransport(
                        item,
                        "UNKNOWN",
                        order,
                        resolveStartTime(activity.startTime()),
                        resolveDuration(activity.duration())
                    ));
                    /*
                    System.out.println("[DB 생략] 이동 이벤트 order=" + order
                            + " start=" + resolveStartTime(activity.startTime())
                            + " duration=" + resolveDuration(activity.duration()));
                     */
                } else {
                    placeRepository.save(new ItineraryItemPlace(
                        item,
                        activity.placeId(),
                        order,
                        resolveStartTime(activity.startTime()),
                        resolveDuration(activity.duration()),
                        resolveCost(activity.cost())
                    ));

                    System.out.println("[DB 생략] 장소 이벤트 order=" + order
                            + " placeId=" + activity.placeId()
                            + " start=" + resolveStartTime(activity.startTime())
                            + " duration=" + resolveDuration(activity.duration())
                            + " cost=" + resolveCost(activity.cost()));
                }
                order++;
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
