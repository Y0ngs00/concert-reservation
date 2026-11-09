package com.concert.concertreservationapi.service;

import com.concert.concertreservationapi.dto.ReservationRequest;
import com.concert.concertreservationapi.entity.*;
import com.concert.concertreservationapi.exception.ReservationForbiddenException;
import com.concert.concertreservationapi.exception.ReservationNotFoundException;
import com.concert.concertreservationapi.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ReservationCancelTest {

    @Autowired
    ReservationService reservationService;
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    SeatRepository seatRepository;
    @Autowired
    ConcertRepository concertRepository;
    @Autowired
    SingerRepository singerRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private Long seatId;
    private Long ownerId;
    private Long otherMemberId;

    private void assertTestDb() {
        String db = jdbcTemplate.queryForObject("select database()", String.class);
        assertThat(db).isEqualTo("concert_reservation_test");
    }

    @BeforeEach
    void setUp() {
        assertTestDb();

        Singer singer = new Singer();
        singer.setSingerName("테스트가수");
        singer = singerRepository.save(singer);

        Concert concert = new Concert();
        concert.setSinger(singer);
        concert.setConcertName("테스트콘서트");
        concert.setConcertSeats(100);
        concert.setVipSeats(10);
        concert.setConcertDay(LocalDate.now().plusDays(30));
        concert.setConcertPrice(new BigDecimal("50000"));
        concert = concertRepository.save(concert);

        Seat seat = new Seat();
        seat.setConcert(concert);
        seat.setSeatNumber(1);
        seat.setSeatGrade("일반");
        seat.setIsReserved(false);
        seat = seatRepository.save(seat);
        seatId = seat.getSeatId();

        Member member = new Member();
        member.setLoginId("user1");
        member.setMemberPw("pw1");
        member.setMemberName("회원1");
        member.setMemberMail("user1@test.com");
        member.setMemberVip(false);
        member.setMemberDate(LocalDateTime.now());
        ownerId = memberRepository.save(member).getMemberId();

        Member other = new Member();
        other.setLoginId("user2");
        other.setMemberPw("pw2");
        other.setMemberName("회원2");
        other.setMemberMail("user2@test.com");
        other.setMemberVip(false);
        other.setMemberDate(LocalDateTime.now());
        otherMemberId = memberRepository.save(other).getMemberId();

    }

    @AfterEach
    void tearDown() {
        assertTestDb();
        reservationRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        concertRepository.deleteAllInBatch();
        singerRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    private Reservation givenReservation(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        seat.setIsReserved(true);
        seatRepository.saveAndFlush(seat);

        Reservation r = new Reservation();
        r.setMember(member);
        r.setSeat(seat);
        r.setReservedAt(LocalDateTime.now());
        r.setReservationStatus(ReservationStatus.RESERVED);
        return reservationRepository.saveAndFlush(r);
    }

    @Test
    void 본인이_예약을_취소하면_상태가_CANCELED이고_좌석이_해제된다() {
        Reservation reservation = givenReservation(ownerId);

        reservationService.cancel(ownerId, reservation.getReservationId());

        Reservation saved = reservationRepository.findById(reservation.getReservationId()).orElseThrow();
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        assertThat(saved.getReservationStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(saved.getCanceledAt()).isNotNull();
        assertThat(seat.getIsReserved()).isFalse();
    }

    @Test
    void 취소_후_다른_회원이_같은_좌석을_예약할_수_있다() {
        Reservation reservation = givenReservation(ownerId);
        reservationService.cancel(ownerId, reservation.getReservationId());

        ReservationRequest request = new ReservationRequest();
        request.setSeatId(seatId);
        reservationService.reserve(otherMemberId, request);

        long reservedCount = reservationRepository.findAll().stream()
                .filter(r -> r.getReservationStatus() == ReservationStatus.RESERVED)
                .count();
        assertThat(reservationRepository.count()).isEqualTo(2);
        assertThat(reservedCount).isEqualTo(1);
    }

    @Test
    void 다른_회원의_예약을_취소하면_거부된다() {
        Reservation reservation = givenReservation(ownerId);

        assertThatThrownBy(() -> reservationService.cancel(otherMemberId, reservation.getReservationId()))
                .isInstanceOf(ReservationForbiddenException.class);

        Reservation saved = reservationRepository.findById(reservation.getReservationId()).orElseThrow();
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        assertThat(saved.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(seat.getIsReserved()).isTrue();
    }

    @Test
    void 이미_취소된_예약을_다시_취소하면_거부된다() {
        Reservation reservation = givenReservation(ownerId);
        reservationService.cancel(ownerId, reservation.getReservationId());

        assertThatThrownBy(() -> reservationService.cancel(ownerId, reservation.getReservationId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 예약입니다.");
    }

    @Test
    void 존재하지_않는_예약을_취소하면_거부된다() {
        assertThatThrownBy(() -> reservationService.cancel(ownerId, 999999L))
                .isInstanceOf(ReservationNotFoundException.class);
    }

}
