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
    @JoinColumn(name = "itinerary_item_id", nullable = false)
    private ItineraryItem itineraryItem;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_time", nullable = false)
    private LocalTime durationTime;

    @Column(name = "cost", nullable = false)
    private BigDecimal cost;

    protected ItineraryItemPlace() {
    }

    public ItineraryItemPlace(
            ItineraryItem itineraryItem,
            String placeId,
            Integer eventOrder,
            LocalTime startTime,
            LocalTime durationTime,
            BigDecimal cost
    ) {
        this.itineraryItem = itineraryItem;
        this.placeId = placeId;
        this.eventOrder = eventOrder;
        this.startTime = startTime;
        this.durationTime = durationTime;
        this.cost = cost;
    }

    public Long getId() {
        return id;
    }
}
