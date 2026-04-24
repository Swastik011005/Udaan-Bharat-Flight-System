package com.udaanbharat.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "flight")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private Integer flightId;

    @Column(name = "flight_number", unique = true, length = 20)
    private String flightNumber;

    // FK → airport.airport_id (origin)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "origin_airport_id")
    private Airport originAirport;

    // FK → airport.airport_id (destination)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_airport_id")
    private Airport destinationAirport;

    

    @Column(name = "departure_time")
    @Temporal(TemporalType.TIME)
    private java.sql.Time departureTime;

    @Column(name = "arrival_time")
    @Temporal(TemporalType.TIME)
    private java.sql.Time arrivalTime;

    @Column(name = "duration", length = 20)
    private String duration;

    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "available_seats")
    private Integer availableSeats;

    @Column(name = "economy_price")
    private Double economyPrice;

    @Column(name = "business_price")
    private Double businessPrice;

    @Column(name = "status", length = 20)
    private String status;

    // Getters and Setters
    public Integer getFlightId() { return flightId; }
    public void setFlightId(Integer flightId) { this.flightId = flightId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public Airport getOriginAirport() { return originAirport; }
    public void setOriginAirport(Airport originAirport) { this.originAirport = originAirport; }

    public Airport getDestinationAirport() { return destinationAirport; }
    public void setDestinationAirport(Airport destinationAirport) { this.destinationAirport = destinationAirport; }

    public java.sql.Time getDepartureTime() { return departureTime; }
    public void setDepartureTime(java.sql.Time departureTime) { this.departureTime = departureTime; }

    public java.sql.Time getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(java.sql.Time arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public Double getEconomyPrice() { return economyPrice; }
    public void setEconomyPrice(Double economyPrice) { this.economyPrice = economyPrice; }

    public Double getBusinessPrice() { return businessPrice; }
    public void setBusinessPrice(Double businessPrice) { this.businessPrice = businessPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}