package com.planit.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.planit.domain.trip.config.ItineraryJobProperties;
import com.planit.domain.trip.config.RedisStreamProperties;
import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.AiItineraryResponse;
import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripStatus;
import com.planit.domain.trip.entity.TripTheme;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.repository.WantedPlaceRepository;
import com.planit.domain.trip.service.ItineraryResultListener;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobRepository;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobStatus;
import com.planit.domain.trip.service.redisAccessor.ItineraryJobStreamService;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripCreateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private WantedPlaceRepository wantedPlaceRepository;

    @Autowired
    private ItineraryDayRepository itineraryDayRepository;

    @Autowired
    private ItineraryItemPlaceRepository itineraryItemPlaceRepository;

    @Autowired
    private ItineraryItemTransportRepository itineraryItemTransportRepository;

    @Autowired
    private ItineraryResultListener itineraryResultListener;

    @Autowired
    private ItineraryJobProperties itineraryJobProperties;

    @Autowired
    private RedisStreamProperties redisStreamProperties;

    @MockBean
    private ItineraryJobRepository itineraryJobRepository;

    @MockBean
    private ItineraryJobStreamService itineraryJobStreamService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    private final Map<Long, Map<String, String>> jobStateStore = new HashMap<>();
    private final List<AiItineraryRequest> enqueuedRequests = new ArrayList<>();
    private final List<Map<String, String>> publishedResults = new ArrayList<>();

    private User user;
    private int ackCount;

    @BeforeEach
    void setUp() throws Exception {
        user = userRepository.save(User.builder()
                .loginId("user1")
                .password("hashed")
                .nickname("nick1")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        jobStateStore.clear();
        enqueuedRequests.clear();
        publishedResults.clear();
        ackCount = 0;
        itineraryJobProperties.setStreamEnabled(true);

        @SuppressWarnings("unchecked")
        StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.acknowledge(anyString(), anyString(), nullable(RecordId.class)))
                .thenAnswer(invocation -> {
                    ackCount++;
                    return 1L;
                });

        doAnswer(invocation -> {
            Long tripId = invocation.getArgument(0);
            String now = Instant.now().toString();
            Map<String, String> state = new HashMap<>();
            state.put("status", ItineraryJobStatus.PENDING.name());
            state.put("errorMessage", "");
            state.put("createdAt", now);
            state.put("updatedAt", now);
            jobStateStore.put(tripId, state);
            return null;
        }).when(itineraryJobRepository).initPending(anyLong());

        doAnswer(invocation -> {
            Long tripId = invocation.getArgument(0);
            ItineraryJobStatus status = invocation.getArgument(1);
            String errorMessage = invocation.getArgument(2);
            Map<String, String> state = jobStateStore.computeIfAbsent(tripId, key -> new HashMap<>());
            state.put("status", status.name());
            state.put("errorMessage", errorMessage == null ? "" : errorMessage);
            state.put("updatedAt", Instant.now().toString());
            return null;
        }).when(itineraryJobRepository).updateStatus(anyLong(), any(ItineraryJobStatus.class), nullable(String.class));

        when(itineraryJobRepository.findStatus(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(jobStateStore.get(invocation.getArgument(0))));
        doNothing().when(itineraryJobRepository).expire(anyLong(), anyLong());

        doAnswer(invocation -> {
            AiItineraryRequest request = invocation.getArgument(1);
            enqueuedRequests.add(request);
            return RecordId.of("1-0");
        }).when(itineraryJobStreamService).enqueueJob(anyLong(), any(AiItineraryRequest.class));

        doAnswer(invocation -> {
            Long tripId = invocation.getArgument(0);
            String status = invocation.getArgument(1);
            AiItineraryResponse response = invocation.getArgument(2);
            String errorMessage = invocation.getArgument(3);

            Map<String, String> fields = new HashMap<>();
            fields.put("tripId", String.valueOf(tripId));
            fields.put("status", status);
            fields.put("payload", response == null ? "" : objectMapper.writeValueAsString(response));
            fields.put("errorMessage", errorMessage == null ? "" : errorMessage);
            publishedResults.add(fields);

            MapRecord<String, String, String> record = MapRecord.create(redisStreamProperties.getAiResultsKey(), fields);
            itineraryResultListener.onMessage(record);
            return RecordId.of("2-0");
        }).when(itineraryJobStreamService)
                .publishResult(anyLong(), anyString(), nullable(AiItineraryResponse.class), nullable(String.class));
    }

    @AfterEach
    void tearDown() {
        itineraryJobProperties.setStreamEnabled(false);
    }

    @Test
    @WithMockUser(username = "user1")
    void 일정생성요청_정상흐름() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "title", "부산 2박 3일",
                "arrivalDate", "2024-08-15",
                "arrivalTime", "09:00",
                "departureDate", "2024-08-17",
                "departureTime", "18:00",
                "travelCity", "부산",
                "totalBudget", 120000,
                "travelTheme", List.of("맛집탐방", "힐링"),
                "wantedPlace", List.of("place-id-1", "place-id-2")
        );

        // When
        MvcResult result = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tripId").isNumber())
                .andReturn();

        // Then
        Long tripId = JsonPath.parse(result.getResponse().getContentAsString())
                .read("$.data.tripId", Number.class)
                .longValue();

        Trip trip = tripRepository.findById(tripId).orElseThrow();
        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayIndex(tripId);

        assertThat(trip.getTitle()).isEqualTo("부산 2박 3일");
        assertThat(trip.getUser().getId()).isEqualTo(user.getId());
        assertThat(trip.getStatus()).isEqualTo(TripStatus.DONE);
        assertThat(trip.getThemes()).extracting(TripTheme::getTheme)
                .containsExactlyInAnyOrder("맛집탐방", "힐링");
        assertThat(wantedPlaceRepository.findByTripId(tripId)).hasSize(2);

        assertThat(enqueuedRequests).hasSize(1);
        assertThat(enqueuedRequests.get(0).tripId()).isEqualTo(tripId);
        assertThat(enqueuedRequests.get(0).travelCity()).isEqualTo("부산");
        assertThat(enqueuedRequests.get(0).travelTheme()).containsExactly("맛집탐방", "힐링");
        assertThat(publishedResults).hasSize(1);
        assertThat(jobStateStore.get(tripId).get("status")).isEqualTo(ItineraryJobStatus.SUCCESS.name());
        assertThat(jobStateStore.get(tripId).get("errorMessage")).isEmpty();
        assertThat(ackCount).isEqualTo(1);

        assertThat(days).hasSize(2);
        int placeCount = days.stream()
                .mapToInt(day -> itineraryItemPlaceRepository.findByItineraryDayIdOrderByEventOrder(day.getId()).size())
                .sum();
        int transportCount = days.stream()
                .mapToInt(day -> itineraryItemTransportRepository.findByItineraryDayIdOrderByEventOrder(day.getId()).size())
                .sum();
        assertThat(placeCount).isEqualTo(4);
        assertThat(transportCount).isEqualTo(2);

        List<ItineraryItemPlace> firstDayPlaces =
                itineraryItemPlaceRepository.findByItineraryDayIdOrderByEventOrder(days.get(0).getId());
        assertThat(firstDayPlaces).isNotEmpty();
        assertThat(firstDayPlaces.get(0).getGooglePlaceId()).isEqualTo("google-place-1");
    }

    @Test
    @WithMockUser(username = "user1")
    void 일정생성요청_입력예외() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "title", "",
                "arrivalDate", "2024-08-15",
                "arrivalTime", "09:00",
                "departureDate", "2024-08-17",
                "departureTime", "18:00",
                "travelCity", "부산",
                "totalBudget", 120000,
                "travelTheme", List.of("맛집탐방")
        );

        long tripCountBefore = tripRepository.count();
        long itineraryDayCountBefore = itineraryDayRepository.count();

        // When
        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("COMMON_001"))
                .andExpect(jsonPath("$.error.code").value("COMMON_001"))
                .andExpect(jsonPath("$.error.message").value("잘못된 요청입니다"));

        // Then
        assertThat(tripRepository.count()).isEqualTo(tripCountBefore);
        assertThat(itineraryDayRepository.count()).isEqualTo(itineraryDayCountBefore);
        assertThat(jobStateStore).isEmpty();
        assertThat(enqueuedRequests).isEmpty();
        assertThat(publishedResults).isEmpty();
        assertThat(ackCount).isZero();
    }
}
