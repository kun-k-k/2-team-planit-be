package com.planit.domain.trip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "itinerary_item_places")
public class ItineraryItemPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    private ItineraryDay itineraryDay;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "place_name", length = 255)
    private String placeName;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_time", nullable = false)
    private LocalTime durationTime;

    @Column(name = "cost", nullable = false)
    private BigDecimal cost;

    @Column(name = "memo")
    private String memo;

    @Column(name = "google_map_url", length = 500)
    private String googleMapUrl;

    @Column(name = "google_place_id", length = 255)
    private String googlePlaceId;

    protected ItineraryItemPlace() {
    }

    public ItineraryItemPlace(
            ItineraryDay itineraryDay,
            String placeId,
            String placeName,
            String type,
            Integer eventOrder,
            LocalTime startTime,
            LocalTime durationTime,
            BigDecimal cost,
            String memo,
            String googleMapUrl,
            String googlePlaceId
    ) {
        this.itineraryDay = itineraryDay;
        this.placeId = placeId;
        this.placeName = placeName;
        this.type = type;
        this.eventOrder = eventOrder;
        this.startTime = startTime;
        this.durationTime = durationTime;
        this.cost = cost;
        this.memo = memo;
        this.googleMapUrl = googleMapUrl;
        this.googlePlaceId = googlePlaceId;
    }

    public Long getId() {
        return id;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getType() {
        return type;
    }

    public Integer getEventOrder() {
        return eventOrder;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getDurationTime() {
        return durationTime;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getMemo() {
        return memo;
    }

    public String getGoogleMapUrl() {
        return googleMapUrl;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public void updatePlaceName(String placeName) {
        this.placeName = placeName;
    }

    public void updatePlaceId(String placeId) {
        this.placeId = placeId;
    }

    public void updateGoogleMapUrl(String googleMapUrl) {
        this.googleMapUrl = googleMapUrl;
    }

    public void updateGooglePlaceId(String googlePlaceId) {
        this.googlePlaceId = googlePlaceId;
    }

    public void updateStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void updateDurationTime(LocalTime durationTime) {
        this.durationTime = durationTime;
    }

    public void updateCost(BigDecimal cost) {
        this.cost = cost;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
