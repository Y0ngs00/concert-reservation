package com.concert.concertreservationapi;

import com.concert.concertreservationapi.entity.Singer;
import com.concert.concertreservationapi.repository.SingerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ConcertReservationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertReservationApiApplication.class, args);
    }

}
