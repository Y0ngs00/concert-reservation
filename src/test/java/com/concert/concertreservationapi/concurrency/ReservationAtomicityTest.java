package com.concert.concertreservationapi.concurrency;

import com.concert.concertreservationapi.dto.ReservationRequest;
import com.concert.concertreservationapi.entity.*;
import com.concert.concertreservationapi.repository.*;
import com.concert.concertreservationapi.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class ReservationAtomicityTest {

    @Autowired ReservationService reservationService;
    @MockitoSpyBean ReservationRepository reservationRepository;
    @MockitoSpyBean SeatRepository seatRepository;
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
    void 예약_저장이_실패하면_좌석_상태도_원래대로_돌아와야_한다() {
        assertThat(seatId).isNotNull();
        assertThat(memberId).isNotNull();

        doThrow(new RuntimeException("예약 저장 실패(테스트용)"))
                .when(reservationRepository).save(any(Reservation.class));

        ReservationRequest request = new ReservationRequest();
        request.setSeatId(seatId);

        assertThatThrownBy(() -> reservationService.reserve(memberId, request))
                .isInstanceOf(RuntimeException.class)
                .hasStackTraceContaining("예약 저장 실패(테스트용)");

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        assertThat(seat.getIsReserved()).isFalse();
        assertThat(reservationRepository.count()).isEqualTo(0);
    }

}
