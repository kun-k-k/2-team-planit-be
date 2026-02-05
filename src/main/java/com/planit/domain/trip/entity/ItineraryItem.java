package com.planit.domain.trip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "itinerary_items")
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "travel_id", nullable = false)
    private Trip trip;

    @Column(name = "day_index", nullable = false)
    private Integer dayIndex;

    protected ItineraryItem() {
    }

    public ItineraryItem(Trip trip, Integer dayIndex) {
        this.trip = trip;
        this.dayIndex = dayIndex;
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
}
