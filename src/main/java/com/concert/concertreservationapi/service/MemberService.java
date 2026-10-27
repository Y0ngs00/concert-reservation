package com.concert.concertreservationapi.service;

import com.concert.concertreservationapi.dto.LoginRequest;
import com.concert.concertreservationapi.dto.MemberRequest;
import com.concert.concertreservationapi.entity.Member;
import com.concert.concertreservationapi.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long register(MemberRequest request) {

        Member member = new Member();
        member.setLoginId(request.getLoginId());
        member.setMemberPw(passwordEncoder.encode(request.getMemberPw()));
        member.setMemberName(request.getMemberName());
        member.setMemberMail(request.getMemberMail());
        member.setMemberVip(false);
        member.setMemberDate(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);
        return savedMember.getMemberId();
    }

    public Long login(LoginRequest request) {

        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(request.getMemberPw(), member.getMemberPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return member.getMemberId();
    }

}
