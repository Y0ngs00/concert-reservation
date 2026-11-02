package com.concert.concertreservationapi.controller;

import com.concert.concertreservationapi.dto.ReservationRequest;
import com.concert.concertreservationapi.dto.ReservationResponse;
import com.concert.concertreservationapi.service.ReservationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ReservationResponse reserveSeat(@AuthenticationPrincipal Long memberId,
                                           @RequestBody ReservationRequest request) {
        request.setMemberId(memberId);
        return reservationService.reserve(request);
    }
}
