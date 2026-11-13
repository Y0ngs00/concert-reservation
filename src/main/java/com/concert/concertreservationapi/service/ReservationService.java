package com.concert.concertreservationapi.service;

import com.concert.concertreservationapi.dto.ReservationRequest;
import com.concert.concertreservationapi.dto.ReservationResponse;
import com.concert.concertreservationapi.entity.Member;
import com.concert.concertreservationapi.entity.Reservation;
import com.concert.concertreservationapi.entity.ReservationStatus;
import com.concert.concertreservationapi.entity.Seat;
import com.concert.concertreservationapi.exception.ReservationForbiddenException;
import com.concert.concertreservationapi.exception.ReservationNotFoundException;
import com.concert.concertreservationapi.repository.MemberRepository;
import com.concert.concertreservationapi.repository.ReservationRepository;
import com.concert.concertreservationapi.repository.SeatRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
        Seat seat = seatRepository.findById(request.getSeatId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        //3. 이미 예약된 좌석인지 체크
        if (seat.getIsReserved()) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }

        //4 좌석 선점(상태 변경 후 즉시 반영, version 충돌은 여기서 판정)
        seat.setIsReserved(true);
        seatRepository.save(seat);
        try {
            seatRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }

        //5. 예약 정보 생성 및 저장
        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setSeat(seat);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setReservationStatus(ReservationStatus.RESERVED);

        try {
            reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }

        return new ReservationResponse(
                reservation.getReservationId(),
                member.getMemberName(),
                seat.getSeatNumber(),
                reservation.getReservedAt(),
                reservation.getReservationStatus().name()
        );
    }

    @Transactional
    public ReservationResponse cancel(Long memberId, Long reservationId) {
        // 1) 예약 조회. 없으면 ReservationNotFoundException("존재하지 않는 예약입니다.")
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("존재하지 않는 예약입니다."));

        // 2) 소유자 확인. 다르면 ReservationForbiddenException("본인의 예약만 취소할 수 있습니다.")
        if (!reservation.getMember().getMemberId().equals(memberId)) {
            throw new ReservationForbiddenException("본인의 예약만 취소할 수 있습니다.");
        }

        // 3) 상태 변경 (이미 취소면 여기서 409용 예외)
        reservation.cancel(LocalDateTime.now());

        // 4) 좌석 해제
        Seat seat = reservation.getSeat();
        seat.setIsReserved(false);
        seatRepository.save(seat);
        try {
            seatRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("이미 처리 중인 취소 요청입니다.");
        }

        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getMember().getMemberName(),
                reservation.getSeat().getSeatNumber(),
                reservation.getReservedAt(),
                reservation.getReservationStatus().name());
    }

}
