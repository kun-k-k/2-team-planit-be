package com.planit.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planit.domain.trip.entity.ItineraryDay;
import com.planit.domain.trip.entity.ItineraryItemPlace;
import com.planit.domain.trip.entity.ItineraryItemTransport;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.ItineraryItemTransportRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripItineraryQueryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ItineraryDayRepository itineraryDayRepository;

    @Autowired
    private ItineraryItemPlaceRepository placeRepository;

    @Autowired
    private ItineraryItemTransportRepository transportRepository;

    private User user;
    private Trip trip;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .loginId("user1")
                .password("hashed")
                .nickname("nick1")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        trip = tripRepository.save(new Trip(
                user,
                "부산 2박 3일",
                LocalDate.of(2024, 8, 15),
                LocalDate.of(2024, 8, 17),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "부산",
                120000
        ));

        ItineraryDay day = itineraryDayRepository.save(new ItineraryDay(
                trip,
                1,
                LocalDate.of(2024, 8, 15).atStartOfDay()
        ));

        placeRepository.save(new ItineraryItemPlace(
                day,
                "place-1",
                "해운대 맛집",
                "Restaurant",
                1,
                LocalTime.of(9, 0),
                LocalTime.of(1, 0),
                BigDecimal.valueOf(10000),
                "메모",
                "https://map.example/1",
                "google-place-1"
        ));

        transportRepository.save(new ItineraryItemTransport(
                day,
                "bus",
                "route",
                2,
                LocalTime.of(10, 30),
                LocalTime.of(0, 30)
        ));
    }

    @Test
    @WithMockUser(username = "user1")
    void getItineraries_happyPath_returnsItineraryAndKeepsDb() throws Exception {
        long beforeDays = itineraryDayRepository.count();
        long beforePlaces = placeRepository.count();
        long beforeTransports = transportRepository.count();

        mockMvc.perform(get("/trips/{tripId}/itineraries", trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tripId").value(trip.getId()))
                .andExpect(jsonPath("$.data.isOwner").value(true))
                .andExpect(jsonPath("$.data.itineraries[0].day").value(1))
                .andExpect(jsonPath("$.data.itineraries[0].activities[0].placeName").value("해운대 맛집"))
                .andExpect(jsonPath("$.data.itineraries[0].activities[0].googleMapUrl").value("https://map.example/1"))
                .andExpect(jsonPath("$.data.itineraries[0].activities[0].googlePlaceId").value("google-place-1"))
                .andExpect(jsonPath("$.data.itineraries[0].activities[1].transport").value("bus"));

        assertThat(itineraryDayRepository.count()).isEqualTo(beforeDays);
        assertThat(placeRepository.count()).isEqualTo(beforePlaces);
        assertThat(transportRepository.count()).isEqualTo(beforeTransports);
    }

    @Test
    @WithMockUser(username = "user1")
    void getItineraries_notFound_returns404AndNoSideEffects() throws Exception {
        long beforeTrips = tripRepository.count();

        mockMvc.perform(get("/trips/{tripId}/itineraries", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("TRIP_001"))
                .andExpect(jsonPath("$.error.code").value("TRIP_001"))
                .andExpect(jsonPath("$.error.message").value("여행을 찾을 수 없습니다"));

        assertThat(tripRepository.count()).isEqualTo(beforeTrips);
    }
}
