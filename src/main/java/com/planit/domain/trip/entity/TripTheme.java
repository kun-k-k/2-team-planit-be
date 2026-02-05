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
@Table(name = "trip_themes")
public class TripTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 50)
    private String theme;

    protected TripTheme() {
    }

    public TripTheme(Trip trip, String theme) {
        this.trip = trip;
        this.theme = theme;
    }

    public Long getId() {
        return id;
    }

    public String getTheme() {
        return theme;
    }
}
