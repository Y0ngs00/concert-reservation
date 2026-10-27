package com.concert.concertreservationapi.dto;

import java.time.LocalDateTime;

public class ReservationResponse {

    private Long reservationId;
    private String memberName;
    private Integer seatNumber;
    private LocalDateTime reservedAt;
    private String reservationStatus;

    public ReservationResponse(Long reservationId, String memberName, Integer seatNumber, LocalDateTime reservedAt, String reservationStatus) {
        this.reservationId = reservationId;
        this.memberName = memberName;
        this.seatNumber = seatNumber;
        this.reservedAt = reservedAt;
        this.reservationStatus = reservationStatus;
    }

    public Long getReservationId() {return reservationId;}
    public String getMemberName() {return memberName;}
    public Integer getSeatNumber() {return seatNumber;}
    public LocalDateTime getReservedAt() {return reservedAt;}
    public String getReservationStatus() {return reservationStatus;}

}
