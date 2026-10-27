package com.concert.concertreservationapi.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "concert")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id")
    private Integer concertId;

    @ManyToOne
    @JoinColumn(name = "singer_id", nullable = false)
    private Singer singer;

    @Column(name = "concert_name", nullable = false, length = 50)
    private String concertName;

    @Column(name = "concert_seats", nullable = false)
    private Integer concertSeats;

    @Column(name = "vip_seats", nullable = false)
    private Integer vipSeats;

    @Column(name = "concert_day", nullable = false)
    private LocalDate concertDay;

    @Column(name = "concert_price", nullable = false)
    private BigDecimal concertPrice;

    public Concert() {
    }

    public Integer getConcertId() {
        return concertId;
    }

    public Singer getSinger() {
        return singer;
    }

    public void setSinger(Singer singer) {
        this.singer = singer;
    }

    public String getConcertName() {
        return concertName;
    }

    public void setConcertName(String concertName) {
        this.concertName = concertName;
    }

    public Integer getConcertSeats() {
        return concertSeats;
    }

    public void setConcertSeats(Integer concertSeats) {
        this.concertSeats = concertSeats;
    }

    public Integer getVipSeats() {
        return vipSeats;
    }

    public void setVipSeats(Integer vipSeats) {
        this.vipSeats = vipSeats;
    }

    public LocalDate getConcertDay() {
        return concertDay;
    }

    public void setConcertDay(LocalDate concertDay) {
        this.concertDay = concertDay;
    }

    public BigDecimal getConcertPrice() {
        return concertPrice;
    }

    public void setConcertPrice(BigDecimal concertPrice) {
        this.concertPrice = concertPrice;
    }
}
