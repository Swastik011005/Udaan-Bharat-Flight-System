package com.udaanbharat.airline.controller;

import com.udaanbharat.airline.entity.Seat;
import com.udaanbharat.airline.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin(origins = "*")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    // GET /api/seats/{flightId}
    @GetMapping("/{flightId}")
    public ResponseEntity<?> getSeatsByFlight(@PathVariable Integer flightId) {
        try {
            List<Seat> seats = seatService.getSeatsByFlight(flightId);
            // Map to simple response (avoid lazy load issues)
            List<Map<String, Object>> result = seats.stream().map(s -> Map.<String, Object>of(
                "seatId",     s.getSeatId(),
                "seatNumber", s.getSeatNumber() != null ? s.getSeatNumber() : "",
                "status",     s.getStatus() != null ? s.getStatus() : "AVAILABLE"
            )).toList();
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/seats/book  — called internally after booking; exposed for manual use
    @PutMapping("/book")
    public ResponseEntity<?> bookSeat(@RequestBody Map<String, Integer> body) {
        try {
            Integer seatId = body.get("seatId");
            if (seatId == null) return ResponseEntity.badRequest().body(Map.of("error", "seatId required"));
            Seat seat = seatService.bookSeat(seatId);
            return ResponseEntity.ok(Map.of(
                "seatId",     seat.getSeatId(),
                "seatNumber", seat.getSeatNumber(),
                "status",     seat.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}