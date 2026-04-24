package com.udaanbharat.airline.dto;

import java.util.List;

/**
 * DTO for POST /api/bookings
 * Backward-compatible: passengers null/empty → single-pax legacy path.
 */
public class BookingRequest {

    private Integer flightId;
    private Integer seatId;          // primary seat (single-pax OR first in multi-pax)
    private Double  amount;
    private String  paymentMethod;
    private List<PassengerEntry> passengers;

    /**
     * ISSUE 1+2 FIX: The actual flight date the user searched for.
     * Format: YYYY-MM-DD  (e.g. "2026-04-24")
     * Sent by frontend from currentSelectedDate so travel date is persisted.
     */
    private String travelDate;

    // ── Passenger sub-DTO ─────────────────────────────────────────────────────
    public static class PassengerEntry {
        private String  name;
        private String  email;
        private Integer seatId;

        public String  getName()           { return name; }
        public void    setName(String n)   { this.name = n; }
        public String  getEmail()          { return email; }
        public void    setEmail(String e)  { this.email = e; }
        public Integer getSeatId()         { return seatId; }
        public void    setSeatId(Integer s){ this.seatId = s; }
    }

    // Getters and Setters
    public Integer getFlightId()  { return flightId; }
    public void    setFlightId(Integer f) { this.flightId = f; }
    public Integer getSeatId()    { return seatId; }
    public void    setSeatId(Integer s)   { this.seatId = s; }
    public Double  getAmount()    { return amount; }
    public void    setAmount(Double a)    { this.amount = a; }
    public String  getPaymentMethod()     { return paymentMethod; }
    public void    setPaymentMethod(String m) { this.paymentMethod = m; }
    public List<PassengerEntry> getPassengers()  { return passengers; }
    public void setPassengers(List<PassengerEntry> p) { this.passengers = p; }
    public String  getTravelDate()        { return travelDate; }
    public void    setTravelDate(String d){ this.travelDate = d; }
}