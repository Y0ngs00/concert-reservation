# concert-reservation

같은 좌석에 동시에 몰리는 예약 요청을 한 명만 성공하도록 막고, 테스트로 확인한 콘서트 좌석 예약 API

> 개발 방식: AI의 안내를 받아 단계별로 구현하며 개념을 학습했습니다. 핵심 단계(5-0~5-3)를 직접 다시 구현하는 학습을 계획하고 있습니다.
## 문제 정의

- 같은 좌석에 100명이 동시에 예약하면, 락이 없을 때 평균 56.4명이 성공해 중복 예약이 생긴다.
- 예약 저장이 실패해도 좌석 상태가 어긋나면 안 된다(원자성).
- 예약을 취소하면 좌석이 풀리고, 취소 후 다른 회원이 다시 예약할 수 있어야 한다.
- 테스트가 개발 DB를 건드리지 않도록 테스트 전용 DB를 분리한다.

## 기술 스택

- Java 21, Spring Boot 4.1.1, Maven
- Spring Web MVC(REST API), Spring Data JPA(Hibernate), MySQL 8.0, HikariCP
- Spring Security, JWT(jjwt), BCrypt
- 테스트: JUnit 5, AssertJ
- API 수동 검증: Postman

## 동시성 제어 5단계 요약

100개 스레드가 같은 좌석 1개를 동시에 예약하는 테스트를 5회 반복한 평균입니다.

| 단계 | 방식 | success | conflict | other | 평균 시간 |
|---|---|---|---|---|---|
| 5-0 | 락 없음 | 56.4 | 43.6 | 0 | 1286 ms |
| 5-1 | `@Transactional` | 1 | 90 | 9 (데드락) | 1144 ms |
| 5-2 | 비관적 락 | 1 | 99 | 0 | 1045 ms |
| 5-3 | 낙관적 락 `@Version` | 1 | 99 | 0 | 884.2 ms |
| 5-4 | + DB 조건부 UNIQUE | 1 | 99 | 0 | 미측정 |

- 시간에는 준비 단계가 포함돼 단계 간 우열을 단정하지 않습니다.
- 최종 전략은 낙관적 락(1차)과 DB 조건부 UNIQUE(2차)의 이중 방어입니다.
- 취소(5-5)는 20개 스레드가 같은 예약을 동시에 취소해 성공 1건, 충돌 19건을 확인했습니다.
- 자세한 측정과 해석: [docs/concurrency-results.md](docs/concurrency-results.md)

## API 목록

| 메서드 | 경로 | 인증 | 설명 | 응답 |
|---|---|---|---|---|
| POST | `/members` | 불필요 | 회원 가입 | 회원 id |
| POST | `/login` | 불필요 | 로그인 | JWT 토큰 문자열 |
| GET | `/concerts/{concertId}/seats` | 불필요 | 콘서트의 좌석 목록 조회 | 좌석 목록 |
| POST | `/reservations` | 필요 | 좌석 예약(회원은 토큰에서 확인) | 예약 정보 |
| PATCH | `/reservations/{reservationId}/cancel` | 필요 | 본인 예약 취소 | 예약 정보(CANCELED) |

- 취소 오류 응답: 없는 예약 404, 남의 예약 403, 이미 취소됨 409.

## 테스트

| 테스트 | 검증 내용 |
|---|---|
| `ReservationConcurrencyTest` | 100명이 같은 좌석을 동시에 예약하면 1명만 성공 |
| `ReservationAtomicityTest` | 예약 저장이 실패하면 좌석 상태도 원래대로 복구 |
| `ReservationDbGuardTest` | DB가 중복 예약을 거부하고, 취소 후 재예약은 허용 |
| `ReservationCancelTest` | 본인 취소, 남의 예약, 이미 취소, 없는 예약, 취소 후 재예약 |
| `ReservationCancelConcurrencyTest` | 20개 스레드가 같은 예약을 취소하면 1건만 성공 |
| `TestDbConnectionTest` | 테스트 전용 DB 연결 확인 |

## 실행 방법

1. MySQL에 `concert_reservation` 스키마를 만들고 `docs/schema.sql`을 적용한다.
2. `src/main/resources/application.properties.example`을 같은 폴더에 `application.properties`로 복사하고 DB 계정과 JWT 키를 입력한다(이 파일은 gitignore).
3. 테스트용 `concert_reservation_test` 스키마를 만들고 같은 `docs/schema.sql`을 적용한다.
4. 서버 실행: IDE에서 `ConcertReservationApiApplication` 실행
5. 테스트 실행: `mvnw.cmd test`

## 현재 진행 상황

- 2026.08.02 프로젝트 시작
- 회원 가입과 로그인(JWT) 완료 (태그 `phase4-auth-done`)
- 테스트 DB 분리, 동시성 재현 테스트, 원자성 테스트
- 동시성 제어 5단계: 락 없음, 트랜잭션, 비관적 락, 낙관적 락, DB 가드 (태그 `phase5-0-before` ~ `phase5-4-db-guard`)
- 예약 취소와 동시 취소 테스트 (태그 `phase5-5-cancel`)
- 다음 계획: 5단계 마무리, Docker 배포, 문서 정리

## 배운 점과 한계

- 락이 없으면 중복 예약이 생기고(평균 56.4건), 테스트가 있어야 이를 숫자로 확인할 수 있었다.
- `@Transactional`만으로는 동시성이 해결되지 않았고(데드락 9건), 낙관적 락과 DB 제약을 함께 쓰는 이중 방어를 선택했다.
- 보호 장치를 잠시 꺼서 테스트가 실제로 실패하는지 확인했다(변이 확인).
- 한계: 전체 흐름은 이해했지만 세부 동작의 설명과 직접 구현은 아직 어렵다. 5-0~5-3을 직접 재구현하며 보완할 계획이다.
- 개선 예정: 취소 동시성 보강, 오류 응답 정리(401/403, 409 구분), 로그인 응답을 JSON으로 변경.