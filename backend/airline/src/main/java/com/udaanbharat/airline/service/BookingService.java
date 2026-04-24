package com.udaanbharat.airline.service;

import com.udaanbharat.airline.dto.BookingRequest;
import com.udaanbharat.airline.entity.*;
import com.udaanbharat.airline.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository          bookingRepository;
    private final PassengerRepository        passengerRepository;
    private final FlightRepository           flightRepository;
    private final SeatRepository             seatRepository;
    private final PaymentRepository          paymentRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final EmailService               emailService;
    private final PdfService                 pdfService;

    public BookingService(BookingRepository bookingRepository,
                          PassengerRepository passengerRepository,
                          FlightRepository flightRepository,
                          SeatRepository seatRepository,
                          PaymentRepository paymentRepository,
                          BookingPassengerRepository bookingPassengerRepository,
                          EmailService emailService,
                          PdfService pdfService) {
        this.bookingRepository          = bookingRepository;
        this.passengerRepository        = passengerRepository;
        this.flightRepository           = flightRepository;
        this.seatRepository             = seatRepository;
        this.paymentRepository          = paymentRepository;
        this.bookingPassengerRepository = bookingPassengerRepository;
        this.emailService               = emailService;
        this.pdfService                 = pdfService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE BOOKING
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public Booking createBooking(String passengerEmail, BookingRequest req) {

        // ── 1. Resolve logged-in user ────────────────────────────────────────
        Passenger accountHolder = passengerRepository.findByEmail(passengerEmail)
                .orElseThrow(() -> new RuntimeException("Passenger not found."));

        // ── 2. Resolve flight ────────────────────────────────────────────────
        Flight flight = flightRepository.findById(req.getFlightId())
                .orElseThrow(() -> new RuntimeException("Flight not found: " + req.getFlightId()));

        // ── 3. Determine pax count ───────────────────────────────────────────
        boolean isMultiPax = req.getPassengers() != null && !req.getPassengers().isEmpty();
        int     paxCount   = isMultiPax ? req.getPassengers().size() : 1;

        // ── 4. Seat availability check ───────────────────────────────────────
        if (flight.getAvailableSeats() == null || flight.getAvailableSeats() < paxCount) {
            throw new RuntimeException(
                "Not enough seats available. Requested: " + paxCount +
                ", Available: " + flight.getAvailableSeats());
        }

        // ── 5. Validate no duplicate seat IDs in one request ────────────────
        if (isMultiPax) {
            Set<Integer> seen = new HashSet<>();
            for (BookingRequest.PassengerEntry pe : req.getPassengers()) {
                if (pe.getSeatId() == null) throw new RuntimeException("Missing seatId for a passenger.");
                if (!seen.add(pe.getSeatId()))
                    throw new RuntimeException("Duplicate seat selected: " + pe.getSeatId());
            }
        }

        // ── 6. Parse travel date — ISSUE 1+2 FIX ────────────────────────────
        // travelDate = the date the user searched for (not today)
        if (req.getTravelDate() == null || req.getTravelDate().isBlank()) {
            throw new RuntimeException("travelDate is required.");
        }
        LocalDate travelDate;
        try {
            travelDate = LocalDate.parse(req.getTravelDate());
        } catch (Exception e) {
            throw new RuntimeException("Invalid travelDate format: " + req.getTravelDate());
        }

        // ── 7. Book the primary seat ─────────────────────────────────────────
        Integer primarySeatId = isMultiPax
                ? req.getPassengers().get(0).getSeatId()
                : req.getSeatId();

        Seat primarySeat = seatRepository.findById(primarySeatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + primarySeatId));
        if (!"AVAILABLE".equalsIgnoreCase(primarySeat.getStatus()))
            throw new RuntimeException("Seat " + primarySeat.getSeatNumber() + " is already booked.");

        primarySeat.setStatus("BOOKED");
        seatRepository.save(primarySeat);

        // ── 8. Create master Booking record ──────────────────────────────────
        Booking booking = new Booking();
        booking.setPassenger(accountHolder);
        booking.setFlight(flight);
        booking.setSeat(primarySeat);
        booking.setBookingDate(LocalDateTime.now());
        booking.setTravelDate(travelDate);          // ← ISSUE 1+2 FIX: actual flight date
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

        // ── 9. Save BookingPassenger rows + book remaining seats ──────────────
        List<BookingPassenger> savedPassengers = new ArrayList<>();

        if (isMultiPax) {
            for (int i = 0; i < req.getPassengers().size(); i++) {
                BookingRequest.PassengerEntry pe = req.getPassengers().get(i);
                Seat seat;
                if (i == 0) {
                    seat = primarySeat;
                } else {
                    seat = seatRepository.findById(pe.getSeatId())
                            .orElseThrow(() -> new RuntimeException("Seat not found: " + pe.getSeatId()));
                    if (!"AVAILABLE".equalsIgnoreCase(seat.getStatus()))
                        throw new RuntimeException("Seat " + seat.getSeatNumber() + " is already booked.");
                    seat.setStatus("BOOKED");
                    seatRepository.save(seat);
                }
                BookingPassenger bp = new BookingPassenger();
                bp.setBooking(booking);
                bp.setPassengerName(pe.getName());
                bp.setPassengerEmail(pe.getEmail());
                bp.setSeatId(seat.getSeatId());
                bp.setSeatNumber(seat.getSeatNumber());
                savedPassengers.add(bookingPassengerRepository.save(bp));
            }
        } else {
            BookingPassenger bp = new BookingPassenger();
            bp.setBooking(booking);
            bp.setPassengerName(accountHolder.getName());
            bp.setPassengerEmail(accountHolder.getEmail());
            bp.setSeatId(primarySeat.getSeatId());
            bp.setSeatNumber(primarySeat.getSeatNumber());
            savedPassengers.add(bookingPassengerRepository.save(bp));
        }

        // ── 10. Decrement available seats ────────────────────────────────────
        flight.setAvailableSeats(flight.getAvailableSeats() - paxCount);
        flightRepository.save(flight);

        // ── 11. Payment record ────────────────────────────────────────────────
        String method = req.getPaymentMethod() != null ? req.getPaymentMethod().toUpperCase() : "CARD";
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(req.getAmount() != null ? req.getAmount() : 0.0);
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentMethod(method);
        paymentRepository.save(payment);

        // ── 12. ISSUE 6 FIX: Extract ALL data as plain strings INSIDE the
        //        transaction, before the session closes. This prevents
        //        LazyInitializationException in the async email thread. ─────
        final EmailDispatchData emailData = buildEmailDispatchData(
                booking, savedPassengers, flight, payment, travelDate);

        // Fire-and-forget email (non-blocking; failures never roll back booking)
        try {
            dispatchEmailsAsync(emailData);
        } catch (Exception e) {
            log.error("Email dispatch init failed for booking #{}: {}", booking.getBookingId(), e.getMessage());
        }

        return booking;
    }

    /**
     * ISSUE 6 FIX: All data extracted as plain Java strings/primitives
     * while the Hibernate session is still open (inside @Transactional).
     * The async thread receives only this plain record — no entity proxies.
     */
    private EmailDispatchData buildEmailDispatchData(Booking booking,
                                                     List<BookingPassenger> passengers,
                                                     Flight flight,
                                                     Payment payment,
                                                     LocalDate travelDate) {
        // Passenger lines — already plain strings from BookingPassenger
        List<EmailService.PassengerLine> paxLines = passengers.stream()
                .map(bp -> new EmailService.PassengerLine(
                        nvl(bp.getPassengerName()), nvl(bp.getSeatNumber())))
                .toList();

        // All flight data extracted NOW while session is open
        String fn       = nvl(flight.getFlightNumber());
        String fromCode = flight.getOriginAirport()      != null ? nvl(flight.getOriginAirport().getCode())      : "";
        String fromCity = flight.getOriginAirport()      != null ? nvl(flight.getOriginAirport().getCity())      : "";
        String toCode   = flight.getDestinationAirport() != null ? nvl(flight.getDestinationAirport().getCode()) : "";
        String toCity   = flight.getDestinationAirport() != null ? nvl(flight.getDestinationAirport().getCity()) : "";
        String dep      = flight.getDepartureTime() != null ? fmtTime(flight.getDepartureTime().toString()) : "—";
        String arr      = flight.getArrivalTime()   != null ? fmtTime(flight.getArrivalTime().toString())   : "—";
        String dur      = nvl(flight.getDuration());
        String travelDateStr = travelDate != null ? travelDate.toString() : "";

        // Payment data
        double  amount    = payment.getAmount() != null ? payment.getAmount() : 0.0;
        String  payMethod = nvl(payment.getPaymentMethod());

        // Deduplicated recipient list (email → name)
        List<EmailService.RecipientEntry> recipients = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (BookingPassenger bp : passengers) {
            String addr = bp.getPassengerEmail();
            if (addr != null && !addr.isBlank() && seen.add(addr.toLowerCase())) {
                recipients.add(new EmailService.RecipientEntry(addr, nvl(bp.getPassengerName())));
            }
        }

        return new EmailDispatchData(
                booking.getBookingId(), paxLines, recipients,
                fn, fromCode, fromCity, toCode, toCity,
                dep, arr, dur, travelDateStr, amount, payMethod);
    }

    /** Plain data carrier — no Hibernate proxies, safe to pass to async thread. */
    public record EmailDispatchData(
        int     bookingId,
        List<EmailService.PassengerLine>  paxLines,
        List<EmailService.RecipientEntry> recipients,
        String fn, String fromCode, String fromCity,
        String toCode, String toCity,
        String dep, String arr, String dur,
        String travelDate,
        double amount, String payMethod
    ) {}

    /**
     * Generates the PDF synchronously (uses the data carrier, not entities),
     * then dispatches one @Async email per unique recipient.
     */
    private void dispatchEmailsAsync(EmailDispatchData d) {

        log.info("Dispatching emails for booking #" + d.bookingId() +
                ": " + d.recipients().size() + " recipient(s)");

        byte[] pdf = pdfService.generateTicketPdf(
                d.bookingId(), d.fn(), d.fromCode(), d.fromCity(),
                d.toCode(), d.toCity(), d.dep(), d.arr(), d.dur(),
                d.travelDate(), d.paxLines(), d.amount(), d.payMethod()
        );

        if (pdf == null || pdf.length == 0) {
            log.error("PDF generation returned empty for booking #" + d.bookingId());
            pdf = new byte[0]; // safety
        } else {
            log.info("PDF generated for booking #" + d.bookingId() + " (" + pdf.length + " bytes)");
        }

        log.info("Sending emails to " + d.recipients().size() + " recipients");

        for (EmailService.RecipientEntry r : d.recipients()) {

            log.info("Sending to: " + r.email() + " (" + r.name() + ")");

            EmailService.EmailData emailData = new EmailService.EmailData(
                    r.email(), r.name(),
                    d.fn(), d.fromCode(), d.fromCity(), d.toCode(), d.toCity(),
                    d.dep(), d.arr(), d.dur(), d.travelDate(),
                    d.paxLines(), d.bookingId(), pdf
            );

            emailService.sendConfirmationEmail(emailData);
        }

        log.info("Email dispatch complete for booking #" + d.bookingId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET USER BOOKINGS
   
    public List<Booking> getUserBookings(Integer passengerId) {
        List<Booking> bookings = bookingRepository.findFullBookingsByPassenger(passengerId);
        autoCompleteBookings(bookings);
        return bookings;
    }

    /**
     * ISSUE 1+2 FIX: Auto-complete only when travelDate == today AND
     * departure time has passed. Without travelDate, fall back to time-only
     * check (original behaviour) to avoid completing future bookings.
     */
    @Transactional
    void autoCompleteBookings(List<Booking> bookings) {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        for (Booking b : bookings) {
            if (!"CONFIRMED".equals(b.getStatus()) && !"CHECKED_IN".equals(b.getStatus())) continue;
            if (b.getFlight() == null || b.getFlight().getDepartureTime() == null) continue;

            LocalDate travelDate = b.getTravelDate();
            LocalTime dep        = b.getFlight().getDepartureTime().toLocalTime();

            boolean shouldComplete;
            if (travelDate != null) {
                // Correct path: travel date known — only complete if today is the travel date
                // and the departure time has passed
                shouldComplete = travelDate.isEqual(today) && now.isAfter(dep);
                // Also complete if travel date is in the PAST
                if (travelDate.isBefore(today)) shouldComplete = true;
            } else {
                // Legacy fallback (no travel_date column value): original time-only logic
                shouldComplete = now.isAfter(dep);
            }

            if (shouldComplete) {
                b.setStatus("COMPLETED");
                bookingRepository.save(b);
            }
        }
    }

    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void scheduledAutoComplete() {
        autoCompleteBookings(bookingRepository.findAllActiveBookings());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CANCEL
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public Booking cancelBooking(Integer bookingId, String passengerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found."));

        if (!booking.getPassenger().getEmail().equals(passengerEmail))
            throw new RuntimeException("Unauthorized.");
        if ("CANCELLED".equals(booking.getStatus()))
            throw new RuntimeException("Booking is already cancelled.");

        List<BookingPassenger> bps = bookingPassengerRepository.findByBookingId(bookingId);
        int released = 0;

        if (!bps.isEmpty()) {
            for (BookingPassenger bp : bps) {
                if (bp.getSeatId() != null) {
                    seatRepository.findById(bp.getSeatId()).ifPresent(s -> {
                        s.setStatus("AVAILABLE");
                        seatRepository.save(s);
                    });
                    released++;
                }
            }
        } else {
            Seat s = booking.getSeat();
            s.setStatus("AVAILABLE");
            seatRepository.save(s);
            released = 1;
        }

        booking.getFlight().setAvailableSeats(booking.getFlight().getAvailableSeats() + released);
        flightRepository.save(booking.getFlight());

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        paymentRepository.findByBooking_BookingId(bookingId).ifPresent(p -> {
            p.setPaymentStatus("REFUNDED");
            paymentRepository.save(p);
        });

        return booking;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHECK-IN
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public Booking checkinBooking(Integer bookingId, String lastName) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found."));
        validateLastName(b.getPassenger().getName(), lastName);
        checkCanCheckin(b);
        b.setStatus("CHECKED_IN");
        return bookingRepository.save(b);
    }

    @Transactional
    public Booking checkinByFlightNumber(String flightNumber, String lastName) {
        List<Booking> bookings = bookingRepository.findActiveByFlightNumber(flightNumber);
        if (bookings.isEmpty())
            throw new RuntimeException("No active booking found for flight " + flightNumber + ".");
        Booking matched = bookings.stream()
                .filter(b -> lastNameMatches(b.getPassenger().getName(), lastName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Last name '" + lastName + "' does not match any booking on flight " + flightNumber + "."));
        checkCanCheckin(matched);
        matched.setStatus("CHECKED_IN");
        return bookingRepository.save(matched);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void validateLastName(String fullName, String lastName) {
        if (fullName == null) return;
        String[] parts = fullName.trim().split("\\s+");
        if (!parts[parts.length - 1].equalsIgnoreCase(lastName.trim()))
            throw new RuntimeException("Last name does not match our records.");
    }

    private boolean lastNameMatches(String fullName, String lastName) {
        if (fullName == null) return false;
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1].equalsIgnoreCase(lastName.trim());
    }

    private void checkCanCheckin(Booking b) {
        switch (nvl(b.getStatus())) {
            case "CANCELLED"  -> throw new RuntimeException("Cannot check in a cancelled booking.");
            case "CHECKED_IN" -> throw new RuntimeException("Already checked in.");
            case "COMPLETED"  -> throw new RuntimeException("This flight has already departed.");
            case "CONFIRMED"  -> { /* OK */ }
            default           -> throw new RuntimeException("Booking is not in a check-in eligible state.");
        }
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    /** "08:30:00" → "8:30 AM" */
    private static String fmtTime(String t) {
        try {
            String hhmm = t.substring(0, 5);
            int h = Integer.parseInt(hhmm.substring(0, 2));
            String m = hhmm.substring(3);
            String ampm = h < 12 ? "AM" : "PM";
            int h12 = h == 0 ? 12 : h > 12 ? h - 12 : h;
            return String.format("%d:%s %s", h12, m, ampm);
        } catch (Exception e) { return t; }
    }
}