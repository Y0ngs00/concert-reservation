package com.concert.concertreservationapi.concurrency;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ReservationCancelConcurrencyTest {

    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ConcertRepository concertRepository;
    @Autowired SingerRepository singerRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long seatId;
    private Long ownerId;
    private Long reservationId;

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
        seat.setIsReserved(true);
        seat = seatRepository.save(seat);
        seatId = seat.getSeatId();

        Member member = new Member();
        member.setLoginId("owner");
        member.setMemberPw("pw");
        member.setMemberName("소유자");
        member.setMemberMail("owner@test.com");
        member.setMemberVip(false);
        member.setMemberDate(LocalDateTime.now());
        member = memberRepository.save(member);
        ownerId = member.getMemberId();

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setSeat(seat);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setReservationStatus(ReservationStatus.RESERVED);
        reservationId = reservationRepository.save(reservation).getReservationId();
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
    void 같은_예약을_동시에_취소하면_1건만_성공해야_한다() throws InterruptedException {
        int threadCount = 20;
        Long versionBefore = seatRepository.findById(seatId).orElseThrow().getVersion();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        Queue<String> otherTypes = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    reservationService.cancel(ownerId, reservationId);
                    success.incrementAndGet();
                } catch (IllegalStateException e) {
                    conflict.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                    otherTypes.add(e.getClass().getName());
                }finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        System.out.printf("success=%d conflict=%d other=%d otherTypes=%s%n",
                success.get(), conflict.get(), other.get(), otherTypes);

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threadCount - 1);
        assertThat(other.get()).isEqualTo(0);

        Reservation saved = reservationRepository.findById(reservationId).orElseThrow();
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        assertThat(saved.getReservationStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(saved.getCanceledAt()).isNotNull();
        assertThat(seat.getIsReserved()).isFalse();
        assertThat(seat.getVersion()).isEqualTo(versionBefore + 1);

    }

}
