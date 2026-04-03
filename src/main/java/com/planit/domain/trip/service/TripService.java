package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.dto.TripCreateResult;
import com.planit.domain.trip.dto.TripListResponse;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.TravelMode;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripStatus;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.entity.WantedPlace;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.repository.TripThemeRepository;
import com.planit.domain.trip.repository.WantedPlaceRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {
    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripThemeRepository tripThemeRepository;
    private final WantedPlaceRepository wantedPlaceRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemPlaceRepository itineraryItemPlaceRepository;
    private final ItineraryItemTransportRepository itineraryItemTransportRepository;
    private final ItineraryEnqueueService itineraryEnqueueService;
    private final TripGroupService tripGroupService;
    private final boolean createWindowEnabled;
    private final boolean dailyCreateLimitEnabled;
    private final Map<Long, LocalDate> lastCreateDateByUser = new ConcurrentHashMap<>();

    public TripService(
            TripRepository tripRepository,
            UserRepository userRepository,
            TripThemeRepository tripThemeRepository,
            WantedPlaceRepository wantedPlaceRepository,
            ItineraryDayRepository itineraryDayRepository,
            ItineraryItemPlaceRepository itineraryItemPlaceRepository,
            ItineraryItemTransportRepository itineraryItemTransportRepository,
            ItineraryEnqueueService itineraryEnqueueService,
            TripGroupService tripGroupService,
            @Value("${trip.create-window-enabled:true}") boolean createWindowEnabled,
            @Value("${trip.daily-create-limit-enabled:true}") boolean dailyCreateLimitEnabled
    ) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripThemeRepository = tripThemeRepository;
        this.wantedPlaceRepository = wantedPlaceRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryItemPlaceRepository = itineraryItemPlaceRepository;
        this.itineraryItemTransportRepository = itineraryItemTransportRepository;
        this.itineraryEnqueueService = itineraryEnqueueService;
        this.tripGroupService = tripGroupService;
        this.createWindowEnabled = createWindowEnabled;
        this.dailyCreateLimitEnabled = dailyCreateLimitEnabled;
    }

    @Transactional
    public TripCreateResult createTrip(TripCreateRequest request, String loginId) {
        log.info("[TRIP_CREATE] start loginId={}, title={}", loginId, request.title());
        if (createWindowEnabled && !isCreateWindowOpen(ZonedDateTime.now(ZoneId.of("Asia/Seoul")))) {
            log.warn("[TRIP_CREATE] blocked by create window loginId={}", loginId);
            throw new BusinessException(ErrorCode.TRIP_005);
        }

        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        log.info("[TRIP_CREATE] user resolved userId={}", user.getId());

        if (dailyCreateLimitEnabled) {
            LocalDate today = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate();
            LocalDate lastCreateDate = lastCreateDateByUser.get(user.getId());
            if (today.equals(lastCreateDate)) {
                log.warn("[TRIP_CREATE] daily limit hit userId={}", user.getId());
                throw new BusinessException(ErrorCode.TRIP_007);
            }
        }

        TravelMode travelMode = request.travelMode() == null ? TravelMode.SOLO : request.travelMode();
        TripStatus initialStatus = travelMode == TravelMode.GROUP ? TripStatus.WAITING : TripStatus.GENERATING;

        Trip trip = tripRepository.save(new Trip(
                user,
                request.title(),
                request.arrivalDate(),
                request.departureDate(),
                request.arrivalTime(),
                request.departureTime(),
                request.travelCity(),
                request.totalBudget(),
                travelMode == TravelMode.GROUP ? request.headCount() : null,
                initialStatus
        ));
        log.info("[TRIP_CREATE] trip saved tripId={}, mode={}", trip.getId(), travelMode);

        for (String theme : request.travelTheme()) {
            trip.addTheme(new TripTheme(trip, theme));
        }
        tripRepository.save(trip);
        log.info("[TRIP_CREATE] themes saved tripId={}, count={}", trip.getId(), trip.getThemes().size());

        if (request.wantedPlace() != null) {
            for (String placeId : request.wantedPlace()) {
                wantedPlaceRepository.save(new WantedPlace(trip, placeId));
            }
            log.info("[TRIP_CREATE] wanted places saved tripId={}, count={}", trip.getId(), request.wantedPlace().size());
        }

        String inviteCode = null;
        if (travelMode == TravelMode.GROUP) {
            // 1) GROUP 모드 생성 시 즉시 itinerary 생성하지 않고 WAITING 그룹을 만든다.
            // 2) 이후 각 멤버 submit이 모여 TripGroupService에서 큐 enqueue를 수행한다.
            inviteCode = tripGroupService.createWaitingGroup(
                    trip,
                    user,
                    request.headCount(),
                    request.travelTheme(),
                    request.wantedPlace()
            );
            log.info("[TRIP_CREATE] group waiting created tripId={}, inviteCode={}", trip.getId(), inviteCode);
        } else {
            //잡큐잉 지점
            itineraryEnqueueService.enqueueGeneration(trip, request.travelTheme(), request.wantedPlace());
        }

        if (dailyCreateLimitEnabled) {
            lastCreateDateByUser.put(user.getId(), ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate());
            log.info("[TRIP_CREATE] daily limit timestamp stored userId={}", user.getId());
        }

        log.info("[TRIP_CREATE] end tripId={}", trip.getId());
        return new TripCreateResult(trip.getId(), inviteCode);
    }

    private boolean isCreateWindowOpen(ZonedDateTime now) {
        LocalTime currentTime = now.toLocalTime();
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(2, 0);
        return !currentTime.isBefore(start) || currentTime.isBefore(end);
    }

    @Transactional
    public void deleteTrip(String loginId, Long tripId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_001));
        if (!trip.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.TRIP_006);
        }

        deleteTripData(tripId);
        tripRepository.delete(trip);
    }

    @Transactional(readOnly = true)
    public TripListResponse getUserTrips(String loginId) {
        // [출력 핵심] owner + group member(참여자) 여행을 함께 조회한다.
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        List<TripListResponse.TripSummary> summaries = tripRepository.findReadableTripsByUserIdOrderByIdDesc(user.getId())
                .stream()
                .map(trip -> new TripListResponse.TripSummary(
                        trip.getId(),
                        trip.getTitle(),
                        trip.getArrivalDate(),
                        trip.getDepartureDate(),
                        trip.getTravelCity()
                ))
                .collect(Collectors.toList());

        return new TripListResponse(summaries);
    }

    private void deleteTripData(Long tripId) {
        List<ItineraryDay> days = itineraryDayRepository.findByTripId(tripId);
        List<Long> dayIds = days.stream().map(ItineraryDay::getId).toList();
        if (!dayIds.isEmpty()) {
            itineraryItemPlaceRepository.deleteByItineraryDayIdIn(dayIds);
            itineraryItemTransportRepository.deleteByItineraryDayIdIn(dayIds);
        }
        itineraryDayRepository.deleteAll(days);

        tripThemeRepository.deleteByTripId(tripId);
        wantedPlaceRepository.deleteByTripId(tripId);
    }
}
