package com.planit.domain.trip.service;

import com.planit.domain.trip.config.ItineraryJobProperties;
import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.entity.Trip;
import java.util.List;

import com.planit.domain.trip.service.AiAccessor.AiItineraryClient;
import com.planit.domain.trip.service.AiAccessor.AiItineraryJob;
import com.planit.domain.trip.service.AiAccessor.AiItineraryQueue;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobService;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobStatus;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ItineraryEnqueueService {
    private static final Logger log = LoggerFactory.getLogger(ItineraryEnqueueService.class);

    private final ItineraryJobService itineraryJobService;
    private final ItineraryJobStreamService itineraryJobStreamService;
    private final AiItineraryClient aiItineraryClient;
    private final AiItineraryQueue aiItineraryQueue;
    private final AiItineraryProcessor aiItineraryProcessor;
    private final ItineraryJobProperties itineraryJobProperties;
    private final boolean aiMockEnabled;

    public ItineraryEnqueueService(
            ItineraryJobService itineraryJobService,
            ItineraryJobStreamService itineraryJobStreamService,
            AiItineraryClient aiItineraryClient,
            AiItineraryQueue aiItineraryQueue,
            AiItineraryProcessor aiItineraryProcessor,
            ItineraryJobProperties itineraryJobProperties,
            Environment environment
    ) {
        this.itineraryJobService = itineraryJobService;
        this.itineraryJobStreamService = itineraryJobStreamService;
        this.aiItineraryClient = aiItineraryClient;
        this.aiItineraryQueue = aiItineraryQueue;
        this.aiItineraryProcessor = aiItineraryProcessor;
        this.itineraryJobProperties = itineraryJobProperties;
        this.aiMockEnabled = Boolean.parseBoolean(environment.getProperty("ai.mock-enabled", "false"));
    }

    public void enqueueGeneration(Trip trip, List<String> themes, List<String> wantedPlaces) {
        AiItineraryRequest request = new AiItineraryRequest(
                trip.getId(),
                trip.getArrivalDate(),
                trip.getArrivalTime(),
                trip.getDepartureDate(),
                trip.getDepartureTime(),
                trip.getTravelCity(),
                trip.getTotalBudget(),
                themes,
                wantedPlaces
        );
        AiItineraryJob job = new AiItineraryJob(request);

        boolean streamEnabled = itineraryJobProperties.isStreamEnabled();
        log.info("[TRIP_CREATE] job built tripId={}, streamEnabled={}, mockEnabled={}", trip.getId(), streamEnabled, aiMockEnabled);
        if (streamEnabled) {
            itineraryJobService.initPending(trip.getId());
            log.info("[TRIP_CREATE] job status initialized PENDING tripId={}", trip.getId());
            itineraryJobStreamService.enqueueJob(trip.getId(), job.request());
            log.info("[TRIP_CREATE] job enqueued to stream tripId={}", trip.getId());

            // AI모킹 지점
            if (aiMockEnabled) {
                AiItineraryResponse response = aiItineraryClient.requestItinerary(job.request());
                log.info("[TRIP_CREATE] mock response generated tripId={}, itineraries={}",
                        trip.getId(), response == null ? 0 : (response.itineraries() == null ? 0 : response.itineraries().size()));
                itineraryJobStreamService.publishResult(trip.getId(), ItineraryJobStatus.SUCCESS.name(), response, null);
                log.info("[TRIP_CREATE] mock result published tripId={}", trip.getId());
            }
        } else {
            if (aiMockEnabled) {
                log.info("[TRIP_CREATE] stream disabled, mock direct processing tripId={}", trip.getId());
                aiItineraryProcessor.process(job);
            } else {
                log.info("[TRIP_CREATE] stream disabled, enqueue in-memory tripId={}", trip.getId());
                aiItineraryQueue.enqueue(job);
            }
        }
    }
}
