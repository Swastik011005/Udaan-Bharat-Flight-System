package com.udaanbharat.airline.dto;
 
public class SeatBookRequest {
    private Integer seatId;
    private Integer bookingId;
 
    public Integer getSeatId() { return seatId; }
    public void setSeatId(Integer seatId) { this.seatId = seatId; }
    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }
}