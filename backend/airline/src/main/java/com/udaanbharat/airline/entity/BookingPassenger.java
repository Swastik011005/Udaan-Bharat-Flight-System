package com.udaanbharat.airline.entity;

import jakarta.persistence.*;

/**
 * Represents one traveller on a multi-passenger booking.
 * One Booking → many BookingPassenger rows.
 *
 * Schema (run migration_booking_passenger.sql before starting the app):
 *   CREATE TABLE booking_passenger (
 *     id               INT AUTO_INCREMENT PRIMARY KEY,
 *     booking_id       INT NOT NULL,
 *     passenger_name   VARCHAR(100) NOT NULL,
 *     passenger_email  VARCHAR(100),
 *     seat_id          INT,
 *     seat_number      VARCHAR(10),
 *     CONSTRAINT fk_bp_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
 *   );
 */
@Entity
@Table(name = "booking_passenger")
public class BookingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "passenger_name", length = 100, nullable = false)
    private String passengerName;

    @Column(name = "passenger_email", length = 100)
    private String passengerEmail;

    /** FK to seat.seat_id — stored for reference, nullable for legacy rows */
    @Column(name = "seat_id")
    private Integer seatId;

    /** Denormalised seat number for fast receipt display without join */
    @Column(name = "seat_number", length = 10)
    private String seatNumber;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

    public Integer getSeatId() { return seatId; }
    public void setSeatId(Integer seatId) { this.seatId = seatId; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
}