package com.udaanbharat.airline.repository;
 
import com.udaanbharat.airline.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
 
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    List<Seat> findByFlight_FlightId(Integer flightId);
    Optional<Seat> findByFlight_FlightIdAndSeatNumber(Integer flightId, String seatNumber);
    Optional<Seat> findBySeatIdAndStatus(Integer seatId, String status);
}
 