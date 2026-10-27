package com.concert.concertreservationapi.repository;


import com.concert.concertreservationapi.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
