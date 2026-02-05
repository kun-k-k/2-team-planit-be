package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.TripCreateRequest;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final AiItineraryQueue aiItineraryQueue;

    public TripService(TripRepository tripRepository, AiItineraryQueue aiItineraryQueue) {
        this.tripRepository = tripRepository;
        this.aiItineraryQueue = aiItineraryQueue;
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
        System.out.println("여행 저장 후 trip객체 반환받음 ---> "+trip.getId());



        // Themes: Trip 1 : Theme N
        for (String theme : request.travelTheme()) {
            trip.addTheme(new TripTheme(trip, theme));
        }
        //System.out.println("테마 목록 할당 후 여행 재저장함");
        tripRepository.save(trip);

        // Enqueue AI request and return immediately
        aiItineraryQueue.enqueue(new AiItineraryJob(new AiItineraryRequest(
                trip.getId(),
                trip.getArrivalDate(),
                trip.getArrivalTime(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getTravelCity(),
                trip.getTotalBudget(),
                request.travelTheme()
        )));

        return trip.getId();

    }


    















}
