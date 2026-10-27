package com.concert.concertreservationapi.dto;

public class SeatResponse {

    private Long seatId;
    private Integer seatNumber;
    private String seatGrade;
    private Boolean isReserved;

    public SeatResponse(Long seatId, Integer seatNumber, String seatGrade, Boolean isReserved){
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.seatGrade = seatGrade;
        this.isReserved = isReserved;
    }

    public Long getSeatId() {return seatId;}
    public Integer getSeatNumber() {return seatNumber;}
    public String getSeatGrade() {return seatGrade;}
    public Boolean getIsReserved() {return isReserved;}

}
