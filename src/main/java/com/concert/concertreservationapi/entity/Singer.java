package com.concert.concertreservationapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "singer")
public class Singer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "singer_id")
    private Integer singerId;

    @Column(name = "singer_name", nullable = false, length = 20)
    private String singerName;

    public Singer() {
    }

    public Integer getSingerId() {
        return singerId;
    }

    public String getSingerName() {
        return singerName;
    }

    public void setSingerName(String singerName) {
        this.singerName = singerName;
    }

}
