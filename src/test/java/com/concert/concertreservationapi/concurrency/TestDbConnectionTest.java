package com.concert.concertreservationapi.concurrency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class TestDbConnectionTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 테스트_프로필은_테스트_DB에_연결된다() {
        String db = jdbcTemplate.queryForObject("select database()", String.class);
        assertThat(db).isEqualTo("concert_reservation_test");
    }

}
