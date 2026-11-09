package com.concert.concertreservationapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 20)
    private ReservationStatus reservationStatus;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    public Reservation() {
    }

    public Long getReservationId() { return reservationId; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }

    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }

    public ReservationStatus getReservationStatus() { return reservationStatus; }
    public void setReservationStatus(ReservationStatus reservationStatus) { this.reservationStatus = reservationStatus; }

    public LocalDateTime getCanceledAt() { return canceledAt; }

    public void cancel(LocalDateTime now) {
        if (this.reservationStatus != ReservationStatus.RESERVED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        this.reservationStatus = ReservationStatus.CANCELED;
        this.canceledAt = now;
    }

}