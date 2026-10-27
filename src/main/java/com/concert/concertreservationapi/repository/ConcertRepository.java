package com.concert.concertreservationapi.repository;

import com.concert.concertreservationapi.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<Concert, Integer> {
}
