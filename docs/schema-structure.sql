-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: concert_reservation
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `concert`
--

DROP TABLE IF EXISTS `concert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concert` (
  `concert_id` int NOT NULL AUTO_INCREMENT,
  `singer_id` int NOT NULL COMMENT 'singer 테이블 참조',
  `concert_name` varchar(50) NOT NULL,
  `concert_seats` int NOT NULL DEFAULT '0' COMMENT '계획된 일반석 수',
  `vip_seats` int NOT NULL DEFAULT '0' COMMENT '계획된 VIP석 수',
  `concert_day` date NOT NULL,
  `concert_price` decimal(10,2) NOT NULL,
  PRIMARY KEY (`concert_id`),
  KEY `FK_CONCERT_SINGER` (`singer_id`),
  KEY `idx_concert_day` (`concert_day`),
  CONSTRAINT `FK_CONCERT_SINGER` FOREIGN KEY (`singer_id`) REFERENCES `singer` (`singer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '회원 고유 식별자',
  `login_id` varchar(20) NOT NULL,
  `member_pw` varchar(255) NOT NULL COMMENT '암호화(BCrypt) 저장',
  `member_name` varchar(10) NOT NULL,
  `member_birth` date DEFAULT NULL COMMENT '연령대 분석용, 선택 입력',
  `member_phone` varchar(20) DEFAULT NULL,
  `member_mail` varchar(100) NOT NULL,
  `member_vip` tinyint(1) NOT NULL DEFAULT '0',
  `member_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `UQ_MEMBER_LOGIN_ID` (`login_id`),
  UNIQUE KEY `UQ_MEMBER_MAIL` (`member_mail`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservation`
--

DROP TABLE IF EXISTS `reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation` (
  `reservation_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT 'member 테이블 참조',
  `seat_id` bigint NOT NULL COMMENT 'seat 테이블 참조',
  `reserved_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reservation_status` varchar(20) NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED 또는 CANCELED',
  PRIMARY KEY (`reservation_id`),
  KEY `FK_RESERVATION_MEMBER` (`member_id`),
  KEY `FK_RESERVATION_SEAT` (`seat_id`),
  CONSTRAINT `FK_RESERVATION_MEMBER` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `FK_RESERVATION_SEAT` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`seat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `seat`
--

DROP TABLE IF EXISTS `seat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seat` (
  `seat_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'DB 내부 관리용 고유번호',
  `concert_id` int NOT NULL COMMENT 'concert 테이블 참조',
  `seat_number` int NOT NULL COMMENT '화면에 보이는 실제 좌석번호',
  `seat_grade` varchar(20) NOT NULL COMMENT 'VIP, 일반, 이코노미',
  `is_reserved` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`seat_id`),
  UNIQUE KEY `UQ_SEAT_CONCERT_NUMBER` (`concert_id`,`seat_number`),
  CONSTRAINT `FK_SEAT_CONCERT` FOREIGN KEY (`concert_id`) REFERENCES `concert` (`concert_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `singer`
--

DROP TABLE IF EXISTS `singer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `singer` (
  `singer_id` int NOT NULL AUTO_INCREMENT,
  `singer_name` varchar(20) NOT NULL,
  PRIMARY KEY (`singer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-02 22:41:59
