package com.planit.domain.trip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "itinerary_days")
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_index", nullable = false)
    private Integer dayIndex;

    @Column(name = "date")
    private LocalDateTime date;

    protected ItineraryDay() {
    }

    public ItineraryDay(Trip trip, Integer dayIndex, LocalDateTime date) {
        this.trip = trip;
        this.dayIndex = dayIndex;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public Integer getDayIndex() {
        return dayIndex;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
