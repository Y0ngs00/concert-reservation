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

## 5-1. `@Transactional` 적용 후

조건은 5-0과 동일합니다(테스트 DB, 100명 동시 요청, 좌석 1개, `spring.jpa.show-sql=false`, HikariCP 기본 풀 10).
변경 사항: `ReservationService.reserve()`에 `@Transactional` 추가

| 회차 | success | conflict | other | reservationRows | 본문 시간(ms) |
|---|---------|----------|-------|-----------------|---------------|
| 1 | 1       | 90       | 9     | 1               | 1130          |
| 2 | 1       | 90       | 9     | 1               | 1204          |
| 3 | 1       | 90       | 9     | 1               | 1166          |
| 4 | 1       | 90       | 9     | 1               | 1108          |
| 5 | 1       | 90       | 9     | 1               | 1112          |
| 평균 | 1       | 90       | 9     | 1               | 1144          |

### 해석
- 원자성 테스트: 5-1 전 실패, 5-1 후 통과
- reservationRows가 5회 모두 1로, 5-0의 중복 예약(43~69건)이 사라졌습니다.
- other 9건은 MySQL 데드락(ErrorCode 1213)으로 롤백된 요청입니다. 풀 크기 10에서 승자 1건을 뺀 값과 일치합니다.
- 데드락 9건은 예약 충돌(409)이 아니라 서버 오류로 처리됩니다. 테스트의 기대값(conflict 99)과 달라 실패로 판정됩니다.
- 판단(추정): 이 결과는 설계된 동시성 제어가 아니라 DB의 데드락 감지에 기댄 것입니다. 풀 크기와 타이밍에 따라 달라질 수 있어 5-2 이후 단계가 필요합니다.

## 5-2. 비관적 락(`PESSIMISTIC_WRITE`) 적용 후

조건은 5-0, 5-1과 동일합니다(테스트 DB, 100명 동시 요청, 좌석 1개, `spring.jpa.show-sql=false`, HikariCP 기본 풀 10).
변경 사항: `SeatRepository`에 `findByIdForUpdate`(`@Lock(PESSIMISTIC_WRITE)`)를 추가하고, `ReservationService.reserve()`의 좌석 조회를 이 메서드로 교체했습니다(`@Transactional` 유지).


| 회차 | success | conflict | other | reservationRows | 본문 시간(ms) |
|---|---------|----------|-------|-----------------|---------------|
| 1 | 1       | 99       | 0     | 1               | 1106          |
| 2 | 1       | 99       | 0     | 1               | 1068          |
| 3 | 1       | 99       | 0     | 1               | 975           |
| 4 | 1       | 99       | 0     | 1               | 1054          |
| 5 | 1       | 99       | 0     | 1               | 1020          |
| 평균 | 1       | 99       | 0     | 1               | 1045          |

### 해석
- 5회 모두 success 1, conflict 99, other 0, reservationRows 1로 테스트 기대값과 일치했고 테스트가 통과했습니다.
- 원자성 테스트: 5-2 적용 후에도 통과(좌석 저장 실패 시 예약 0건 유지)
- 5-1의 데드락(ErrorCode 1213) 9건이 사라졌습니다. 좌석을 처음부터 쓰기 잠금으로 조회해 읽기 잠금에서 쓰기 잠금으로 올리는 과정이 없어졌기 때문으로 추정합니다.
- 충돌 99건은 잠금 대기 후 최신 상태(isReserved=true)를 읽고 예외로 종료된 요청입니다.
- 시간(참고): 평균 1045ms로 5-1(1144ms)보다 낮지만, 표시 시간에 준비 단계가 포함되었을 수 있고 표본이 5회라 차이를 단정하지 않습니다.
- 참고 실행(표 제외): 같은 조건 1회, success 1, conflict 99, other 0, rows 1, 1008ms
- 한계(미검증 추정): 잠금을 쥔 동안 대기 요청이 DB 연결을 점유하므로, 요청 수가 연결 풀보다 훨씬 커지면 연결 부족이 생길 수 있습니다.

## 5-3. 낙관적 락 (@Version)

### 조건
- 테스트 DB `concert_reservation_test`, 좌석 1개, 동시 요청 100건(회원 100명)
- `spring.jpa.show-sql=false`, HikariCP 기본 풀 10, 격리 수준 REPEATABLE READ
- 방식: `Seat`에 `@Version`(jakarta.persistence.Version) 추가. 좌석을 먼저 선점(`UPDATE ... WHERE seat_id=? AND version=?` 후 flush)하고, 그 다음 예약을 INSERT
- 충돌 시 `OptimisticLockingFailureException`을 `IllegalStateException`으로 변환(API는 기존대로 409)

### 결과
| 회차 | success | conflict | other | reservationRows | 본문 시간(ms) |
|---|---------|----------|-------|-----------------|---------------|
| 1 | 1       | 99       | 0     | 1               | 912           |
| 2 | 1       | 99       | 0     | 1               | 880           |
| 3 | 1       | 99       | 0     | 1               | 892           |
| 4 | 1       | 99       | 0     | 1               | 848           |
| 5 | 1       | 99       | 0     | 1               | 889           |
| 평균 | 1       | 99       | 0     | 1               | 884.2         |

### 해석
- 정합성: 5회 모두 1/99/0/1. 좌석 `version`이 0에서 1로 한 번만 올랐음을 테스트로 단언(성공한 UPDATE는 1건).
- 데드락 0건: `success=1`, `conflict=99`가 단언되어 있어 `other`는 0.
- 시간: 평균 884.2ms(표준편차 약 23.4ms). 5-2(1045ms) 대비 약 15% 짧게 측정됐으나, 5회 표본이고 5-3 테스트에는 version 조회가 1회 추가되어 우열 결론은 보류(신뢰도 중간).
- 한계: `conflict`에 "이미 예약됨"과 "낙관적 락 충돌"이 합산되어 있음.

### 참고: 순서 변경 전 관찰 (표 제외)
- 예약 INSERT 후 좌석 UPDATE 순서에서는 1/90/9/1(데드락 1213 확인, other=9).
- 추정 원인(신뢰도 중상): INSERT 시 FK 검사로 좌석 행에 공유 락이 걸린 상태에서 여러 트랜잭션이 좌석 UPDATE의 배타 락을 서로 기다려 교착.
- 조치: 좌석 선점(UPDATE + flush)을 먼저, 예약 INSERT를 나중에 하도록 변경해 해소.

### 원자성 테스트 재설계
- 순서 변경 후 기존 원자성 테스트가 의미를 잃어(항상 통과) 반대 방향으로 교체: 예약 저장이 실패하면 좌석 상태도 롤백되어야 함.
- `@Transactional`을 잠시 주석 처리하면 실패(Red)하고 복원하면 통과(Green)함을 확인.
