package com.concert.concertreservationapi.dto;

public class ReservationRequest {

    private Long memberId;
    private Long seatId;

    public ReservationRequest() {}

    public Long getMemberId() {return memberId;}
    public void setMemberId(Long memberId) {this.memberId = memberId;}

    public Long getSeatId() {return seatId;}
    public void setSeatId(Long seatId) {this.seatId = seatId;}

}
