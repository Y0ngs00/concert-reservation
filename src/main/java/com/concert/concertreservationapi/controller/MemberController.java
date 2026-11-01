package com.concert.concertreservationapi.controller;

import com.concert.concertreservationapi.dto.LoginRequest;
import com.concert.concertreservationapi.dto.MemberRequest;
import com.concert.concertreservationapi.service.MemberService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/members")
    public Long register(@RequestBody MemberRequest request) {
        return memberService.register(request);
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return memberService.login(request);
    }

}
