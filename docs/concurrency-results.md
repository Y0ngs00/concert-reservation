# Phase 5 동시성 제어 결과 기록

## 공통 조건
- 요청: 스레드 100개(CountDownLatch로 동시 출발), 서로 다른 회원 100명이 같은 좌석 1개를 예약
- DB: MySQL 8.0.46, 테스트 전용 스키마(concert_reservation_test)
- 연결 풀: HikariCP 기본값(별도 설정 없음)
- 측정 설정: spring.jpa.show-sql=false
- 테스트: ReservationConcurrencyTest (기대: success 1, conflict 99, reservationRows 1)

## 5-0 Before (락 없음, `@Transactional` 없음)
| 회차 | success | conflict | other | reservationRows | 본문 시간(ms) |
|---|---|---|---|---|---|
| 1 | 63 | 37 | 0 | 63 | 1257 |
| 2 | 64 | 36 | 0 | 64 | 1268 |
| 3 | 69 | 31 | 0 | 69 | 1229 |
| 4 | 43 | 57 | 0 | 43 | 1344 |
| 5 | 43 | 57 | 0 | 43 | 1334 |
| 평균 | 56.4 | 43.6 | 0 | 56.4 | 1286 |

### 해석
- 5회 모두 기대(성공 1)와 달랐고, 성공 건수는 43~69명이었다.
- 성공 건수와 예약 행 수가 매번 같다. 응답과 DB 모두에 같은 좌석의 중복 예약이 남았다.
- 원인: '빈 좌석인지 확인'과 '예약 확정'이 하나의 원자적 동작이 아니어서, 그 사이에 여러 요청이 끼어드는 경쟁 상태(check-then-act)다. reservation.seat_id에 UNIQUE 제약도 없어서 DB가 막지 못한다.
