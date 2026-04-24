package com.udaanbharat.airline.repository;

import com.udaanbharat.airline.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    /**
     * ISSUE 1 FIX: Full JOIN FETCH so no lazy-loading crash when controller
     * maps flight/seat fields, and results are always latest (no cache).
     * Ordered by bookingDate DESC so newest booking appears first.
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.passenger " +
           "JOIN FETCH b.flight f " +
           "JOIN FETCH f.originAirport " +
           "JOIN FETCH f.destinationAirport " +
           "JOIN FETCH b.seat " +
           "WHERE b.passenger.passengerId = :passengerId " +
           "ORDER BY b.bookingDate DESC")
    List<Booking> findFullBookingsByPassenger(@Param("passengerId") Integer passengerId);

    /** Legacy derived query — kept for backward compat with scheduler */
    List<Booking> findByPassenger_PassengerIdOrderByBookingDateDesc(Integer passengerId);

    /** Receipt: fetch single booking with all associations eager */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.passenger " +
           "JOIN FETCH b.flight f " +
           "JOIN FETCH f.originAirport " +
           "JOIN FETCH f.destinationAirport " +
           "JOIN FETCH b.seat " +
           "WHERE b.bookingId = :id")
    Optional<Booking> findByIdWithAllDetails(@Param("id") Integer id);

    /** Check-in by flight number */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.passenger p " +
           "JOIN FETCH b.flight f " +
           "JOIN FETCH f.originAirport " +
           "JOIN FETCH f.destinationAirport " +
           "JOIN FETCH b.seat " +
           "WHERE f.flightNumber = :flightNumber " +
           "AND b.status IN ('CONFIRMED', 'CHECKED_IN') " +
           "ORDER BY b.bookingId DESC")
    List<Booking> findActiveByFlightNumber(@Param("flightNumber") String flightNumber);

    /** Scheduled auto-complete job */
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.flight f " +
           "WHERE b.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Booking> findAllActiveBookings();
}