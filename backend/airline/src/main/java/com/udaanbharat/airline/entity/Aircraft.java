package com.udaanbharat.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "aircraft")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aircraft_id")
    private Integer aircraftId;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "total_seats")
    private Integer totalSeats;

    // Getters and Setters
    public Integer getAircraftId() { return aircraftId; }
    public void setAircraftId(Integer aircraftId) { this.aircraftId = aircraftId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
}