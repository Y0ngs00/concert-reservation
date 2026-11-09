CREATE TABLE `member` (
    `member_id`     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '회원 고유 식별자',
    `login_id`      VARCHAR(20)     NOT NULL,
    `member_pw`     VARCHAR(255)    NOT NULL COMMENT '암호화(BCrypt) 저장',
    `member_name`   VARCHAR(10)     NOT NULL,
    `member_birth`  DATE            NULL COMMENT '연령대 분석용, 선택 입력',
    `member_phone`  VARCHAR(20)     NULL,
    `member_mail`   VARCHAR(100)    NOT NULL,
    `member_vip`    BOOLEAN         NOT NULL DEFAULT FALSE,
    `member_date`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`member_id`),
    UNIQUE KEY `UQ_MEMBER_LOGIN_ID` (`login_id`),
    UNIQUE KEY `UQ_MEMBER_MAIL` (`member_mail`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `singer` (
    `singer_id`     INT             NOT NULL AUTO_INCREMENT,
    `singer_name`   VARCHAR(20)     NOT NULL,
    PRIMARY KEY (`singer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `concert` (
    `concert_id`      INT             NOT NULL AUTO_INCREMENT,
    `singer_id`       INT             NOT NULL COMMENT 'singer 테이블 참조',
    `concert_name`    VARCHAR(50)     NOT NULL,
    `concert_seats`   INT             NOT NULL DEFAULT 0 COMMENT '계획된 일반석 수',
    `vip_seats`       INT             NOT NULL DEFAULT 0 COMMENT '계획된 VIP석 수',
    `concert_day`     DATE            NOT NULL,
    `concert_price`   DECIMAL(10,2)   NOT NULL,
    PRIMARY KEY (`concert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `seat` (
    `seat_id`       BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'DB 내부 관리용 고유번호',
    `concert_id`    INT             NOT NULL COMMENT 'concert 테이블 참조',
    `seat_number`   INT             NOT NULL COMMENT '화면에 보이는 실제 좌석번호',
    `seat_grade`    VARCHAR(20)     NOT NULL COMMENT 'VIP, 일반, 이코노미',
    `is_reserved`   BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (`seat_id`),
    UNIQUE KEY `UQ_SEAT_CONCERT_NUMBER` (`concert_id`, `seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `reservation` (
    `reservation_id`      BIGINT          NOT NULL AUTO_INCREMENT,
    `member_id`           BIGINT          NOT NULL COMMENT 'member 테이블 참조',
    `seat_id`             BIGINT          NOT NULL COMMENT 'seat 테이블 참조',
    `reserved_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `reservation_status`  VARCHAR(20)     NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED 또는 CANCELED',
    PRIMARY KEY (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `concert`
    ADD CONSTRAINT `FK_CONCERT_SINGER` FOREIGN KEY (`singer_id`) REFERENCES `singer` (`singer_id`);

ALTER TABLE `seat`
    ADD CONSTRAINT `FK_SEAT_CONCERT` FOREIGN KEY (`concert_id`) REFERENCES `concert` (`concert_id`);

ALTER TABLE `reservation`
    ADD CONSTRAINT `FK_RESERVATION_MEMBER` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
    ADD CONSTRAINT `FK_RESERVATION_SEAT` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`seat_id`);

-- =====================================================================
-- 변경 이력 (위 CREATE TABLE은 최초 설계이고, 아래는 이후 변경입니다)
-- 적용 대상: 개발 DB(concert_reservation), 테스트 DB(concert_reservation_test)
-- 실행 전에 대상 스키마를 확인하고, DDL은 백업 후에 실행합니다.
-- =====================================================================

-- Phase 5-3: 낙관적 락용 버전 열 (JPA @Version)
ALTER TABLE `seat`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;

-- Phase 5-4 (테스트 DB 2026-11-08, 개발 DB 2026-11-09)
-- 같은 좌석의 RESERVED 예약이 두 건 이상 저장되지 못하게 DB가 막습니다.
-- MySQL 8.0은 부분 인덱스가 없어서, RESERVED일 때만 seat_id 값을 갖는
-- 가상 생성 열에 UNIQUE 인덱스를 걸었습니다. CANCELED 행은 NULL이라
-- 충돌하지 않으므로 취소 후 재예약이 가능합니다.
ALTER TABLE `reservation`
    ADD COLUMN `active_seat_id` BIGINT
        GENERATED ALWAYS AS (CASE WHEN `reservation_status` = 'RESERVED' THEN `seat_id` END) VIRTUAL,
    ADD UNIQUE INDEX `uk_reservation_active_seat` (`active_seat_id`);

-- 되돌리기(필요할 때만):
-- ALTER TABLE `reservation` DROP INDEX `uk_reservation_active_seat`, DROP COLUMN `active_seat_id`;

-- Phase 5-5 (테스트 DB, 개발 DB 모두 2026-11-09)
-- 예약 취소 시각. 취소 전에는 NULL, 취소하면 취소한 시각을 기록합니다.
-- NULL 허용 열이라 이 열을 모르는 이전 코드도 그대로 동작합니다.
ALTER TABLE `reservation`
    ADD COLUMN `canceled_at` DATETIME NULL;

-- 되돌리기(필요할 때만):
-- ALTER TABLE `reservation` DROP COLUMN `canceled_at`;