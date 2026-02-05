package com.planit.domain.trip.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id")
    private Long groupId;

    @Column(nullable = false, length = 15)
    private String title;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "travel_city", length = 50)
    private String travelCity;

    @Column(name = "total_budget")
    private Integer totalBudget;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TripTheme> themes = new ArrayList<>();

    protected Trip() {
    }

    public Trip(
            String title,
            LocalDate arrivalDate,
            LocalDate departureDate,
            LocalTime arrivalTime,
            LocalTime departureTime,
            String travelCity,
            Integer totalBudget
    ) {
        this.title = title;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.travelCity = travelCity;
        this.totalBudget = totalBudget;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public String getTravelCity() {
        return travelCity;
    }

    public Integer getTotalBudget() {
        return totalBudget;
    }

    public List<TripTheme> getThemes() {
        return themes;
    }

    public void addTheme(TripTheme theme) {
        // TripTheme 생성자에서 trip 연관관계를 설정하고, 여기서는 리스트에만 추가
        themes.add(theme);
    }
}
