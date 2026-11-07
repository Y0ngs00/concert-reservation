package com.concert.concertreservationapi.service;

import com.concert.concertreservationapi.dto.ReservationRequest;
import com.concert.concertreservationapi.dto.ReservationResponse;
import com.concert.concertreservationapi.entity.Member;
import com.concert.concertreservationapi.entity.Reservation;
import com.concert.concertreservationapi.entity.Seat;
import com.concert.concertreservationapi.repository.MemberRepository;
import com.concert.concertreservationapi.repository.ReservationRepository;
import com.concert.concertreservationapi.repository.SeatRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReservationService {

    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(MemberRepository memberRepository,
                              SeatRepository seatRepository,
                              ReservationRepository reservationRepository) {
        this.memberRepository = memberRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ReservationResponse reserve(Long memberId, ReservationRequest request) {

        //1. 회원 찾기
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        //2. 좌석 찾기
        Seat seat = seatRepository.findByIdForUpdate(request.getSeatId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        //3. 이미 예약된 좌석인지 체크
        if (seat.getIsReserved()) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }

        //4. 예약 정보 생성 및 저장
        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setSeat(seat);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setReservationStatus("RESERVED");
        reservationRepository.save(reservation);

        //5. 좌석 상태 변경 및 저장
        seat.setIsReserved(true);
        seatRepository.save(seat);

        return new ReservationResponse(
                reservation.getReservationId(),
                member.getMemberName(),
                seat.getSeatNumber(),
                reservation.getReservedAt(),
                reservation.getReservationStatus()
        );
    }

}
