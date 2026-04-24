package com.udaanbharat.airline.repository;

import com.udaanbharat.airline.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Integer> {

    @Query("SELECT f FROM Flight f " +
           "WHERE f.originAirport.code = :originCode " +
           "AND f.destinationAirport.code = :destinationCode " +
           "AND f.availableSeats > 0 " +
           "ORDER BY f.departureTime")
    List<Flight> searchByAirportCodes(@Param("originCode") String originCode,
                                       @Param("destinationCode") String destinationCode);

    Optional<Flight> findByFlightNumber(String flightNumber);
}