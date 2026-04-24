package com.udaanbharat.airline.controller;

import com.udaanbharat.airline.entity.Flight;
import com.udaanbharat.airline.service.FlightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
@CrossOrigin(origins = "*")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public ResponseEntity<?> getAllFlights() {
        return ResponseEntity.ok(
            flightService.getAllFlights().stream()
                .map(f -> toMap(f, null))
                .toList()
        );
    }

    /**
     * Search flights by origin/destination with optional date (YYYY-MM-DD).
     *
     * ISSUE 1 FIX: Backend validates the date param and rejects past dates.
     * This is the server-side enforcement — the frontend already clamps the
     * date picker to today..today+5, but the backend must not trust the client.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String date) {

        // ── Date validation (Issue 1 backend fix) ────────────────────────────
        if (date != null && !date.isBlank()) {
            try {
                LocalDate requestedDate = LocalDate.parse(date);  // expects YYYY-MM-DD
                LocalDate today         = LocalDate.now();

                if (requestedDate.isBefore(today)) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Cannot search flights for a past date. Please select today or a future date.");
                    return ResponseEntity.badRequest().body(err);
                }

                if (requestedDate.isAfter(today.plusDays(365))) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Search date cannot be more than 1 year in the future.");
                    return ResponseEntity.badRequest().body(err);
                }

            } catch (DateTimeParseException ex) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Invalid date format. Expected YYYY-MM-DD, got: " + date);
                return ResponseEntity.badRequest().body(err);
            }
        }

        try {
            List<Map<String, Object>> results = flightService
                    .searchFlights(origin, destination)
                    .stream()
                    .map(f -> toMap(f, date))
                    .toList();
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFlightById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(toMap(flightService.getFlightById(id), null));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** GET /api/flights/status?flightNumber=UB107 */
    @GetMapping("/status")
    public ResponseEntity<?> getFlightStatus(@RequestParam String flightNumber) {
        try {
            Flight f = flightService.getFlightByNumber(flightNumber);
            String computedStatus = computeRealtimeStatus(f);

            Map<String, Object> status = new HashMap<>();
            status.put("flightNumber",   f.getFlightNumber());
            status.put("status",         computedStatus);
            status.put("departure",      f.getDepartureTime() != null ? f.getDepartureTime().toString() : "");
            status.put("arrival",        f.getArrivalTime()   != null ? f.getArrivalTime().toString()   : "");
            status.put("duration",       f.getDuration()      != null ? f.getDuration()                  : "");
            status.put("from",           f.getOriginAirport()      != null ? f.getOriginAirport().getCode()      : "");
            status.put("fromName",       f.getOriginAirport()      != null ? f.getOriginAirport().getName()      : "");
            status.put("to",             f.getDestinationAirport() != null ? f.getDestinationAirport().getCode() : "");
            status.put("toName",         f.getDestinationAirport() != null ? f.getDestinationAirport().getName() : "");
            status.put("availableSeats", f.getAvailableSeats());
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Flight " + flightNumber + " not found.");
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * Real-time status based on current wall clock vs stored departure/arrival times.
     *   now > arrival   → COMPLETED
     *   now > departure → IN_AIR
     *   else            → stored status or ON_TIME
     */
    private String computeRealtimeStatus(Flight f) {
        if (f.getDepartureTime() == null || f.getArrivalTime() == null) {
            return f.getStatus() != null ? f.getStatus() : "ON_TIME";
        }
        LocalTime now = LocalTime.now();
        LocalTime dep = f.getDepartureTime().toLocalTime();
        LocalTime arr = f.getArrivalTime().toLocalTime();

        if (now.isAfter(arr)) return "COMPLETED";
        if (now.isAfter(dep)) return "IN_AIR";
        return (f.getStatus() != null && !f.getStatus().isBlank()) ? f.getStatus() : "ON_TIME";
    }

    private Map<String, Object> toMap(Flight f, String date) {
        Map<String, Object> m = new HashMap<>();
        m.put("flightId",         f.getFlightId());
        m.put("flightNumber",     f.getFlightNumber());
        m.put("departureTime",    f.getDepartureTime() != null ? f.getDepartureTime().toString() : "");
        m.put("arrivalTime",      f.getArrivalTime()   != null ? f.getArrivalTime().toString()   : "");
        m.put("duration",         f.getDuration()      != null ? f.getDuration()                  : "");
        m.put("availableSeats",   f.getAvailableSeats());
        m.put("economyPrice",     f.getEconomyPrice()  != null ? f.getEconomyPrice()  : 0.0);
        m.put("businessPrice",    f.getBusinessPrice() != null ? f.getBusinessPrice() : 0.0);
        m.put("status",           computeRealtimeStatus(f));
        if (f.getOriginAirport() != null) {
            m.put("originCode",  f.getOriginAirport().getCode()  != null ? f.getOriginAirport().getCode()  : "");
            m.put("originName",  f.getOriginAirport().getName()  != null ? f.getOriginAirport().getName()  : "");
            m.put("originCity",  f.getOriginAirport().getCity()  != null ? f.getOriginAirport().getCity()  : "");
        } else { m.put("originCode",""); m.put("originName",""); m.put("originCity",""); }
        if (f.getDestinationAirport() != null) {
            m.put("destinationCode", f.getDestinationAirport().getCode()  != null ? f.getDestinationAirport().getCode()  : "");
            m.put("destinationName", f.getDestinationAirport().getName()  != null ? f.getDestinationAirport().getName()  : "");
            m.put("destinationCity", f.getDestinationAirport().getCity()  != null ? f.getDestinationAirport().getCity()  : "");
        } else { m.put("destinationCode",""); m.put("destinationName",""); m.put("destinationCity",""); }
        return m;
    }
}