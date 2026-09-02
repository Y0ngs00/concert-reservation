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