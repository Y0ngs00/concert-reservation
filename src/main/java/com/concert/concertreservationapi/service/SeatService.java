package com.concert.concertreservationapi.service;

import com.concert.concertreservationapi.dto.SeatResponse;
import com.concert.concertreservationapi.entity.Seat;
import com.concert.concertreservationapi.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public List<SeatResponse> getSeatsByConcert(Integer concertId) {
        List<Seat> seats = seatRepository.findByConcert_ConcertId(concertId);

        return seats.stream()
                .map(seat -> new SeatResponse(
                        seat.getSeatId(),
                        seat.getSeatNumber(),
                        seat.getSeatGrade(),
                        seat.getIsReserved()
                ))
                .collect(Collectors.toList());
    }

}
