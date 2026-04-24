package com.udaanbharat.airline.service;

import com.udaanbharat.airline.entity.Seat;
import com.udaanbharat.airline.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public List<Seat> getSeatsByFlight(Integer flightId) {
        return seatRepository.findByFlight_FlightId(flightId);
    }

    @Transactional
    public Seat bookSeat(Integer seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));

        if (!"AVAILABLE".equalsIgnoreCase(seat.getStatus())) {
            throw new RuntimeException("Seat " + seat.getSeatNumber() + " is not available.");
        }

        seat.setStatus("BOOKED");
        return seatRepository.save(seat);
    }

    @Transactional
    public Seat releaseSeat(Integer seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
        seat.setStatus("AVAILABLE");
        return seatRepository.save(seat);
    }
}