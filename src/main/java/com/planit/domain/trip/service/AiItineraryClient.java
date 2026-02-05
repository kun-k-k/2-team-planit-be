package com.planit.domain.trip.service;

import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.dto.ActivityDto;
import com.planit.domain.trip.dto.ItineraryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class AiItineraryClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean mockEnabled;

    //생성자로 client클래스 필드 주입
    public AiItineraryClient(RestTemplate restTemplate,
                             @Value("${ai.base-url:http://localhost:8000}") String baseUrl,
                             @Value("${ai.mock-enabled:false}") boolean mockEnabled) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.mockEnabled = mockEnabled;
    }

    //ai서버 요청 메서드
    public AiItineraryResponse requestItinerary(AiItineraryRequest request) {
        if (mockEnabled) {
            //throw new RestClientException("ai서버요청 에러응답 테스트용");
            return createDummyResponse(request);
        }
        // RestTemplate: 스프링 기본 HTTP 클라이언트
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiItineraryRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(baseUrl + "/api/v1/itinerary", entity, AiItineraryResponse.class);
        // postForObject = throws RestClientException
    }



    private AiItineraryResponse createDummyResponse(AiItineraryRequest request) {
        LocalDate baseDate = request.arrivalDate() != null ? request.arrivalDate() : LocalDate.now();

        ActivityDto activity1 = new ActivityDto("place", "place-1", LocalTime.of(9, 0), 50000, 120, "");
        ActivityDto activity2 = new ActivityDto("route", null, LocalTime.of(11, 0), null, 30, "");
        ActivityDto activity3 = new ActivityDto("place", "place-2", LocalTime.of(11, 30), 30000, 180, "");
        ItineraryDto day1 = new ItineraryDto(1, baseDate, List.of(activity1, activity2, activity3));

        ActivityDto activity4 = new ActivityDto("place", "place-3", LocalTime.of(10, 0), 40000, 90, "");
        ActivityDto activity5 = new ActivityDto("route", null, LocalTime.of(11, 30), null, 45, "");
        ActivityDto activity6 = new ActivityDto("place", "place-4", LocalTime.of(12, 15), 60000, 150, "");
        ItineraryDto day2 = new ItineraryDto(2, baseDate.plusDays(1), List.of(activity4, activity5, activity6));

        return new AiItineraryResponse("OK", List.of(day1, day2));
    }
}
