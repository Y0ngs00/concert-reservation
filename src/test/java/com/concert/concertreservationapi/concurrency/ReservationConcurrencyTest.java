package com.concert.concertreservationapi.concurrency;

import com.concert.concertreservationapi.dto.ReservationRequest;
import com.concert.concertreservationapi.entity.Concert;
import com.concert.concertreservationapi.entity.Member;
import com.concert.concertreservationapi.entity.Seat;
import com.concert.concertreservationapi.entity.Singer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ReservationConcurrencyTest {

    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ConcertRepository concertRepository;
    @Autowired SingerRepository singerRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long seatId;
    private final List<Long> memberIds = new ArrayList<>();

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

            for (int i = 1; i <= 100; i++) {
                Member member = new Member();
                member.setLoginId("user" + i);
                member.setMemberPw("pw" + i);
                member.setMemberName("회원" + i);
                member.setMemberMail("user" + i + "@test.com");
                member.setMemberVip(false);
                member.setMemberDate(LocalDateTime.now());
                memberIds.add(memberRepository.save(member).getMemberId());
            }

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
        void 같은_좌석에_100명이_동시에_예약하면_1명만_성공해야_한다() throws InterruptedException{
            int threadCount = memberIds.size();
            ReservationRequest request = new ReservationRequest();
            request.setSeatId(seatId);

            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger conflict = new AtomicInteger();
            AtomicInteger other = new AtomicInteger();

            for (Long memberId : memberIds) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        reservationService.reserve(memberId, request);
                        success.incrementAndGet();
                    } catch (IllegalStateException e) {
                        conflict.incrementAndGet();
                    } catch (Exception e) {
                        other.incrementAndGet();
                    }finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
            pool.shutdown();

            long rows = reservationRepository.count();
            System.out.printf("success=%d conflict=%d other=%d reservationRows=%d%n",
                    success.get(), conflict.get(), other.get(), rows);

            assertThat(success.get()).isEqualTo(1);
            assertThat(conflict.get()).isEqualTo(99);
            assertThat(rows).isEqualTo(1);

            Seat seat = seatRepository.findById(seatId).orElseThrow();
            assertThat(seat.getVersion()).isEqualTo(1L);
        }

    }

