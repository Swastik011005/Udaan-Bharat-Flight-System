package com.udaanbharat.airline.service;

import com.udaanbharat.airline.entity.Flight;
import com.udaanbharat.airline.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public List<Flight> searchFlights(String originCode, String destinationCode) {
        return flightRepository.searchByAirportCodes(originCode, destinationCode);
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public Flight getFlightById(Integer id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + id));
    }

    public Flight getFlightByNumber(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + flightNumber));
    }
}