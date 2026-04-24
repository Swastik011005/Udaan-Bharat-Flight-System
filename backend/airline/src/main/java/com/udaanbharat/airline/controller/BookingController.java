package com.udaanbharat.airline.controller;

import com.udaanbharat.airline.dto.BookingRequest;
import com.udaanbharat.airline.entity.Booking;
import com.udaanbharat.airline.entity.BookingPassenger;
import com.udaanbharat.airline.entity.Payment;
import com.udaanbharat.airline.repository.BookingPassengerRepository;
import com.udaanbharat.airline.repository.BookingRepository;
import com.udaanbharat.airline.repository.PaymentRepository;
import com.udaanbharat.airline.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService             bookingService;
    private final PaymentRepository          paymentRepository;
    private final BookingRepository          bookingRepository;
    private final BookingPassengerRepository bookingPassengerRepository;

    public BookingController(BookingService bookingService,
                             PaymentRepository paymentRepository,
                             BookingRepository bookingRepository,
                             BookingPassengerRepository bookingPassengerRepository) {
        this.bookingService             = bookingService;
        this.paymentRepository          = paymentRepository;
        this.bookingRepository          = bookingRepository;
        this.bookingPassengerRepository = bookingPassengerRepository;
    }

    // ── POST /api/bookings ─────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createBooking(
            @AuthenticationPrincipal String email,
            @RequestBody BookingRequest req) {
        try {
            Booking booking = bookingService.createBooking(email, req);
            int paxCount = (req.getPassengers() != null) ? req.getPassengers().size() : 1;

            Map<String, Object> response = new HashMap<>();
            response.put("bookingId",      booking.getBookingId());
            response.put("status",         booking.getStatus());
            response.put("seatNumber",     booking.getSeat() != null ? booking.getSeat().getSeatNumber() : "");
            response.put("flightNumber",   booking.getFlight() != null ? booking.getFlight().getFlightNumber() : "");
            response.put("bookingDate",    booking.getBookingDate() != null ? booking.getBookingDate().toString() : "");
            // ISSUE 1+2 FIX: return travelDate so frontend can display it immediately
            response.put("travelDate",     booking.getTravelDate() != null ? booking.getTravelDate().toString() : "");
            response.put("passengerCount", paxCount);
            response.put("message",        "Booking confirmed for " + paxCount + " passenger(s)!");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/bookings/user/{passengerId} ───────────────────────────────────
    @GetMapping("/user/{passengerId}")
    public ResponseEntity<?> getUserBookings(@PathVariable Integer passengerId) {
        try {
            List<Booking> bookings = bookingService.getUserBookings(passengerId);
            List<Map<String, Object>> result = bookings.stream().map(b -> {
                String from = "", fromCode = "", fromCity = "";
                String to   = "", toCode   = "", toCity   = "";
                if (b.getFlight() != null) {
                    if (b.getFlight().getOriginAirport() != null) {
                        from     = nvl(b.getFlight().getOriginAirport().getName());
                        fromCode = nvl(b.getFlight().getOriginAirport().getCode());
                        fromCity = nvl(b.getFlight().getOriginAirport().getCity());
                    }
                    if (b.getFlight().getDestinationAirport() != null) {
                        to     = nvl(b.getFlight().getDestinationAirport().getName());
                        toCode = nvl(b.getFlight().getDestinationAirport().getCode());
                        toCity = nvl(b.getFlight().getDestinationAirport().getCity());
                    }
                }
                int paxCount = bookingPassengerRepository.findByBookingId(b.getBookingId()).size();
                if (paxCount == 0) paxCount = 1;

                Map<String, Object> row = new HashMap<>();
                row.put("bookingId",      b.getBookingId());
                row.put("bookingDate",    b.getBookingDate() != null ? b.getBookingDate().toString() : "");
                // ISSUE 1+2 FIX: include travelDate in list response
                row.put("travelDate",     b.getTravelDate() != null ? b.getTravelDate().toString() : "");
                row.put("status",         nvl(b.getStatus()));
                row.put("flightNumber",   b.getFlight() != null ? nvl(b.getFlight().getFlightNumber()) : "");
                row.put("from",           from);
                row.put("fromCode",       fromCode);
                row.put("fromCity",       fromCity);
                row.put("to",             to);
                row.put("toCode",         toCode);
                row.put("toCity",         toCity);
                row.put("seatNumber",     b.getSeat() != null ? nvl(b.getSeat().getSeatNumber()) : "");
                row.put("departure",      b.getFlight() != null && b.getFlight().getDepartureTime() != null
                                          ? b.getFlight().getDepartureTime().toString() : "");
                row.put("passengerCount", paxCount);
                return row;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT /api/bookings/{bookingId}/cancel ───────────────────────────────────
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Integer bookingId,
            @AuthenticationPrincipal String email) {
        try {
            Booking b = bookingService.cancelBooking(bookingId, email);
            return ResponseEntity.ok(Map.of("bookingId", b.getBookingId(), "status", b.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/bookings/{bookingId}/checkin ─────────────────────────────────
    @PostMapping("/{bookingId}/checkin")
    public ResponseEntity<?> checkin(
            @PathVariable Integer bookingId,
            @RequestBody Map<String, String> body) {
        try {
            String lastName     = body.get("lastName");
            String flightNumber = body.get("flightNumber");

            if (lastName == null || lastName.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Last name is required."));

            Booking b;
            if (bookingId == 0) {
                if (flightNumber == null || flightNumber.isBlank())
                    return ResponseEntity.badRequest().body(Map.of("error", "Flight number is required."));
                b = bookingService.checkinByFlightNumber(flightNumber.toUpperCase().trim(), lastName);
            } else {
                b = bookingService.checkinBooking(bookingId, lastName);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bookingId",     b.getBookingId());
            response.put("status",        b.getStatus());
            response.put("passengerName", b.getPassenger().getName());
            response.put("flightNumber",  b.getFlight() != null ? b.getFlight().getFlightNumber() : "");
            response.put("from",          b.getFlight() != null && b.getFlight().getOriginAirport() != null
                                          ? b.getFlight().getOriginAirport().getCode() : "");
            response.put("to",            b.getFlight() != null && b.getFlight().getDestinationAirport() != null
                                          ? b.getFlight().getDestinationAirport().getCode() : "");
            response.put("seatNumber",    b.getSeat() != null ? b.getSeat().getSeatNumber() : "");
            response.put("departure",     b.getFlight() != null && b.getFlight().getDepartureTime() != null
                                          ? b.getFlight().getDepartureTime().toString() : "");
            response.put("travelDate",    b.getTravelDate() != null ? b.getTravelDate().toString() : "");
            response.put("gate",          "B" + ((b.getBookingId() % 20) + 1));
            response.put("boardingTime",  "45 mins before departure");
            response.put("message",       "Check-in successful! Your boarding pass is ready.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/bookings/{bookingId}/receipt ──────────────────────────────────
    @GetMapping("/{bookingId}/receipt")
    public ResponseEntity<?> getReceipt(@PathVariable Integer bookingId) {
        try {
            Booking booking = bookingRepository.findByIdWithAllDetails(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found."));

            Payment payment = paymentRepository.findByBooking_BookingId(bookingId).orElse(null);
            List<BookingPassenger> bps = bookingPassengerRepository.findByBookingId(bookingId);

            Map<String, Object> receipt = new HashMap<>();
            receipt.put("bookingId",      booking.getBookingId());
            receipt.put("bookingDate",    booking.getBookingDate() != null ? booking.getBookingDate().toString() : "");
            // ISSUE 7 FIX: include travelDate in receipt response
            receipt.put("travelDate",     booking.getTravelDate() != null ? booking.getTravelDate().toString() : "");
            receipt.put("status",         booking.getStatus());
            receipt.put("passengerName",  booking.getPassenger().getName());
            receipt.put("passengerEmail", booking.getPassenger().getEmail());
            receipt.put("flightNumber",   booking.getFlight().getFlightNumber());
            receipt.put("from",           booking.getFlight().getOriginAirport().getCode());
            receipt.put("fromName",       booking.getFlight().getOriginAirport().getName());
            receipt.put("to",             booking.getFlight().getDestinationAirport().getCode());
            receipt.put("toName",         booking.getFlight().getDestinationAirport().getName());
            receipt.put("departure",      booking.getFlight().getDepartureTime() != null
                                          ? booking.getFlight().getDepartureTime().toString() : "");
            receipt.put("arrival",        booking.getFlight().getArrivalTime() != null
                                          ? booking.getFlight().getArrivalTime().toString() : "");
            receipt.put("duration",       nvl(booking.getFlight().getDuration()));
            receipt.put("seatNumber",     booking.getSeat().getSeatNumber());
            receipt.put("amount",         payment != null ? payment.getAmount()        : 0.0);
            receipt.put("paymentStatus",  payment != null ? nvl(payment.getPaymentStatus()) : "");
            receipt.put("paymentMethod",  payment != null && payment.getPaymentMethod() != null
                                          ? payment.getPaymentMethod() : "CARD");

            if (!bps.isEmpty()) {
                List<Map<String, String>> paxList = bps.stream().map(bp -> {
                    Map<String, String> p = new HashMap<>();
                    p.put("passengerName",  nvl(bp.getPassengerName()));
                    p.put("passengerEmail", nvl(bp.getPassengerEmail()));
                    p.put("seatNumber",     nvl(bp.getSeatNumber()));
                    return p;
                }).collect(Collectors.toList());
                receipt.put("bookingPassengers", paxList);
                receipt.put("passengerCount",    bps.size());
            } else {
                receipt.put("passengerCount", 1);
            }

            return ResponseEntity.ok(receipt);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}