package com.concert.concertreservationapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "안녕하세요, 콘섯트 예약 시스템입니다.";
    }

}
