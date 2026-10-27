package com.concert.concertreservationapi.controller;

import com.concert.concertreservationapi.dto.SeatResponse;
import com.concert.concertreservationapi.entity.Seat;
import com.concert.concertreservationapi.service.SeatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService){
        this.seatService = seatService;
    }

    @GetMapping("/concerts/{concertId}/seats")
    public List<SeatResponse> getSeats(@PathVariable Integer concertId){
        return seatService.getSeatsByConcert(concertId);
    }

}
