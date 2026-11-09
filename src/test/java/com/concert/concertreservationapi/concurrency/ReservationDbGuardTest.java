package com.concert.concertreservationapi.concurrency;

import com.concert.concertreservationapi.entity.*;
import com.concert.concertreservationapi.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ReservationDbGuardTest {

    @Autowired ReservationRepository reservationRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ConcertRepository concertRepository;
    @Autowired SingerRepository singerRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long seatId;
    private Long memberId;

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
        memberId = memberRepository.save(member).getMemberId();

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

    @Test
    void 같은_좌석에_예약중_예약을_두번_저장하면_DB가_거부한다() {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Seat seat = seatRepository.findById(seatId).orElseThrow();

        reservationRepository.saveAndFlush(newReservation(member, seat, ReservationStatus.RESERVED));

        assertThatThrownBy(() ->
                reservationRepository.saveAndFlush(newReservation(member, seat, ReservationStatus.RESERVED)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    void 취소된_예약이_있어도_같은_좌석을_다시_예약할_수_있다() {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Seat seat = seatRepository.findById(seatId).orElseThrow();

        reservationRepository.saveAndFlush(newReservation(member, seat, ReservationStatus.CANCELED));
        reservationRepository.saveAndFlush(newReservation(member, seat, ReservationStatus.RESERVED));

        assertThat(reservationRepository.count()).isEqualTo(2);
    }

    private Reservation newReservation(Member member, Seat seat, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setMember(member);
        r.setSeat(seat);
        r.setReservedAt(LocalDateTime.now());
        r.setReservationStatus(status);
        return r;
    }

}
