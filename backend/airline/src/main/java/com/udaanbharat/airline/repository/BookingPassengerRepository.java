package com.udaanbharat.airline.repository;

import com.udaanbharat.airline.entity.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Integer> {

    /** Fetch all passengers for a given booking — used by receipt endpoint. */
    @Query("SELECT bp FROM BookingPassenger bp WHERE bp.booking.bookingId = :bookingId ORDER BY bp.id")
    List<BookingPassenger> findByBookingId(@Param("bookingId") Integer bookingId);
}