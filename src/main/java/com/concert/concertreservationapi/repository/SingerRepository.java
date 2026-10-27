package com.concert.concertreservationapi.repository;

import com.concert.concertreservationapi.entity.Singer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SingerRepository extends JpaRepository<Singer, Integer> {
}
