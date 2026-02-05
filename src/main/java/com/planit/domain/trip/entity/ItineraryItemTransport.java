package com.planit.domain.trip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "itinerary_item_transports")
public class ItineraryItemTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    private ItineraryDay itineraryDay;

    @Column(name = "transport", nullable = false, length = 20)
    private String transport;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_time", nullable = false)
    private LocalTime durationTime;

    protected ItineraryItemTransport() {
    }

    public ItineraryItemTransport(
            ItineraryDay itineraryDay,
            String transport,
            String type,
            Integer eventOrder,
            LocalTime startTime,
            LocalTime durationTime
    ) {
        this.itineraryDay = itineraryDay;
        this.transport = transport;
        this.type = type;
        this.eventOrder = eventOrder;
        this.startTime = startTime;
        this.durationTime = durationTime;
    }

    public Long getId() {
        return id;
    }

    public String getTransport() {
        return transport;
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
}
