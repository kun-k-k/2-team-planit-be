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
@Table(name = "wanted_places")
public class WantedPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "travel_id", nullable = false)
    private Trip trip;

    @Column(name = "google_map_id", nullable = false, length = 150)
    private String googleMapId;

    protected WantedPlace() {
    }

    public WantedPlace(Trip trip, String googleMapId) {
        this.trip = trip;
        this.googleMapId = googleMapId;
    }

    public Long getId() {
        return id;
    }

    public String getGoogleMapId() {
        return googleMapId;
    }
}
