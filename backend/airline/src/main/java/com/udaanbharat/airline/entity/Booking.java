package com.udaanbharat.airline.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ISSUE 1+2 FIX: Added travelDate (LocalDate) — the actual flight date.
 *
 * DB migration required (run once):
 *   ALTER TABLE booking ADD COLUMN travel_date DATE NULL;
 *
 * Existing rows will have travel_date = NULL; the display falls back to
 * bookingDate's date portion for those rows.
 */
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    /**
     * The actual flight travel date (selected by the user during search).
     * Stored separately from bookingDate because bookingDate = NOW() and
     * the flight may be scheduled days in the future.
     */
    @Column(name = "travel_date")
    private LocalDate travelDate;

    // 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'CHECKED_IN'
    @Column(name = "status", length = 20)
    private String status;

    // Getters and Setters
    public Integer       getBookingId()    { return bookingId; }
    public void          setBookingId(Integer b) { this.bookingId = b; }
    public Passenger     getPassenger()    { return passenger; }
    public void          setPassenger(Passenger p) { this.passenger = p; }
    public Flight        getFlight()       { return flight; }
    public void          setFlight(Flight f) { this.flight = f; }
    public Seat          getSeat()         { return seat; }
    public void          setSeat(Seat s)   { this.seat = s; }
    public LocalDateTime getBookingDate()  { return bookingDate; }
    public void          setBookingDate(LocalDateTime d) { this.bookingDate = d; }
    public LocalDate     getTravelDate()   { return travelDate; }
    public void          setTravelDate(LocalDate d) { this.travelDate = d; }
    public String        getStatus()       { return status; }
    public void          setStatus(String s) { this.status = s; }
}