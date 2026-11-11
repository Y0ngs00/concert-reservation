package com.concert.concertreservationapi.repository;


import com.concert.concertreservationapi.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByConcert_ConcertId(Integer conertId);
}
