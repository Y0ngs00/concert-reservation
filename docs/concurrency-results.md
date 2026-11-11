# Phase 5 동시성 제어 결과 기록

## 공통 조건
- 요청: 스레드 100개(CountDownLatch로 동시 출발), 서로 다른 회원 100명이 같은 좌석 1개를 예약
- DB: MySQL 8.0.46, 테스트 전용 스키마(concert_reservation_test)
- 연결 풀: HikariCP 기본값(별도 설정 없음)
- 측정 설정: spring.jpa.show-sql=false
- 테스트: ReservationConcurrencyTest (기대: success 1, conflict 99, reservationRows 1)

## 5단계 요약 비교

공통 조건: 100 스레드, 좌석 1개, 테스트 DB, HikariCP 기본 풀 10, `show-sql=false`, 5회 평균 (5-5만 20 스레드)

| 단계 | 방식 | success | conflict | other | 예약 행 | 평균 시간 | 판정과 한계 |
|---|---|---|---|---|---|---|---|
| 5-0 | 락 없음 | 56.4 | 43.6 | 0 | 56.4 | 1286 ms | 중복 예약 발생. 확인과 확정 사이의 경쟁 상태 |
| 5-1 | `@Transactional` | 1 | 90 | 9 | 1 | 1144 ms | 데드락 9건이 서버 오류로 처리됨. 설계된 제어가 아님(추정) |
| 5-2 | 비관적 락 | 1 | 99 | 0 | 1 | 1045 ms | 통과. 대기 요청이 연결을 점유해 풀 부족 가능(미검증) |
| 5-3 | 낙관적 락 `@Version` | 1 | 99 | 0 | 1 | 884.2 ms | 통과. conflict에 "이미 예약됨"과 "충돌"이 합산됨 |
| 5-4 | + DB 조건부 UNIQUE | 1 | 99 | 0 | 1 | 미측정 | 통과(전체 6건). 락이 뚫려도 DB가 최종 방어 |
| 5-5 | 취소 (5-3, 5-4 재사용) | 1 | 19 | 0 | - | 약 500 ms | 통과(20 스레드). 보강 후보 미적용 |

시간에는 준비 단계가 포함될 수 있어 단계 간 우열을 단정하지 않는다. 5-3의 표준편차는 약 23.4 ms.

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

## 5-4. DB 수준 중복 예약 방어 (조건부 UNIQUE 인덱스)

### 목적
애플리케이션 락(낙관적 락)이 어떤 이유로 뚫려도, 같은 좌석에 RESERVED 예약이 두 건 저장되지 못하도록 DB가 마지막으로 막는다.

### 설계
- 예약 행을 지우지 않고 reservation_status를 CANCELED로 바꾸는 이력 보존형 취소 구조를 유지한다.
- MySQL 8.0에는 부분 인덱스가 없으므로, reservation_status가 RESERVED일 때만 seat_id 값을 갖는 가상 생성 열 active_seat_id에 UNIQUE 인덱스를 건다.
- CANCELED 행은 active_seat_id가 NULL이고 UNIQUE 인덱스는 NULL끼리 충돌하지 않으므로, 취소 후 같은 좌석을 다시 예약할 수 있다.
- 단순 UNIQUE(seat_id)는 취소 후 재예약까지 막으므로 채택하지 않았다.

### 적용 DDL
(docs/schema.sql 하단 변경 이력 참고)

~~~sql
ALTER TABLE reservation 
    ADD COLUMN active_seat_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN reservation_status = 'RESERVED' THEN seat_id END) VIRTUAL,
    ADD UNIQUE INDEX uk_reservation_active_seat (active_seat_id);
~~~

### 검증 (Red, Green)

| 단계 | 상황 | 결과 |
|---|---|---|
| Red | DDL 적용 전 테스트 DB에서 ReservationDbGuardTest 실행 | 두 번째 RESERVED 저장이 예외 없이 통과해 `Expecting code to raise a throwable`로 실패 |
| Green | 테스트 DB에 DDL 적용 후 같은 테스트 실행 | 2건 통과 (중복 RESERVED 거부, 취소 후 재예약 허용) |
| 회귀 | 전체 테스트 | 6건 통과, 동시성 테스트 결과 1/99/0/1 유지 |

### 개발 DB 기록
- 2026-11-08: 중복 RESERVED 행 정리 (reservation_id 4, 6 삭제, 8건에서 6건으로), 트랜잭션 안에서 삭제 후 COMMIT
- 2026-11-09: DB 백업 후 개발 DB에 같은 DDL 적용, SHOW COLUMNS와 SHOW INDEX로 확인

### 남은 과제
- 중복 저장 시 발생하는 DataIntegrityViolationException을 사용자 메시지로 바꾸는 처리 검토
- 예약 취소 기능(5-5): 완료 (아래 5-5 섹션 참고)

## 5-5 예약 취소 (취소 후 재예약, 동시 취소)

### 목적
예약 취소 기능을 추가하고, 같은 예약을 동시에 취소해도 **한 번만 취소**되며 좌석과 예약 상태가 어긋나지 않는지 증명한다.

### 설계
- API: `PATCH /reservations/{reservationId}/cancel` (회원 id는 토큰에서 추출, 본인 예약만 취소)
- 상태: `ReservationStatus { RESERVED, CANCELED }`, `canceled_at DATETIME NULL` 추가 (이력 보존)
- 순서: 예약 조회(404) → 소유자 확인(403) → 상태 변경(이미 취소면 409) → 좌석 해제(`flush`로 `@Version` 충돌 감지, 충돌 시 409)
- 동시성 보호: 좌석 `@Version`(5-3) + 5-4의 DB 유니크 가드(`CANCELED`는 `active_seat_id`가 NULL이라 재예약 허용)

### 응답 규칙과 수동 검증 (Postman, 개발 DB)
| 상황        | 응답                                    | 결과 |
|-------------|-----------------------------------------|---|
| 정상 취소   | 200, `CANCELED`                         | OK |
| 이미 취소됨 | 409, `이미 취소된 예약입니다.`          | OK (DB의 `canceled_at` 불변 확인) |
| 없는 예약   | 404                                     | OK |
| 남의 예약 | 403 `본인의 예약만 취소할 수 있습니다.` | OK (예약 상태 불변 확인) |
| 토큰 없음 | 403 (스프링 기본 형식)                  | 개선 후보: 401 구분 |

### 테스트
- `ReservationCancelTest` 5건 (정상, 취소 후 재예약, 남의 예약, 이미 취소, 없는 예약)
- `ReservationCancelConcurrencyTest` 1건 (20개 스레드가 같은 예약을 동시에 취소)
- 전체 12건 통과

### 동시 취소 결과 (테스트 DB, 20 스레드)
| 항목 | 값 |
|---|---|
| success | 1 |
| conflict | 19 |
| other | 0 |
| 좌석 version | 정확히 +1 |
| 소요 시간 | 약 0.5초 (성능 비교용 아님) |

### 변이 확인 (테스트가 진짜로 잡는지 증명)
- 좌석의 `@Version`을 일시 제거하고 같은 테스트를 실행: `success=10` (expected 1)로 **실패**
- 복구 후: `success=1`로 통과
- 처음 작성 때 준비 데이터 실수(좌석을 미예약 상태로 생성)로도 `success=10`이 나옴. **좌석 상태가 실제로 바뀌지 않으면 `@Version` 검사가 일어나지 않는다**는 사실을 확인함.

### 보강 후보 (미적용)
- 예약에도 `@Version` 추가, 또는 `UPDATE ... WHERE status='RESERVED'`의 영향 행 수로 원자적 취소

### 개발 DB 반영 기록
- `reservation.canceled_at DATETIME NULL` (11/09 백업 후 적용)
- 수동 검증 중 개발 DB에 연습 예약 15, 16번(CANCELED 이력)이 남음
