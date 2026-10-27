package com.concert.concertreservationapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_MEMBER_LOGIN_ID", columnNames = {"login_id"}),
                @UniqueConstraint(name = "UQ_MEMBER_MAIL", columnNames = {"member_mail"})
        }
)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "login_id", nullable = false, length = 20)
    private String loginId;

    @Column(name = "member_pw", nullable = false, length = 255, comment = "BCrypt")
    private String memberPw;

    @Column(name = "member_name", nullable = false, length = 10)
    private String memberName;

    @Column(name = "member_birth", nullable = true)
    private LocalDate memberBirth;

    @Column(name = "member_phone", nullable = true, length = 20)
    private String memberPhone;

    @Column(name = "member_mail", nullable = false, length = 100)
    private String memberMail;

    @Column(name = "member_vip", nullable = false)
    private boolean memberVip;

    @Column(name = "member_date", nullable = false)
    private LocalDateTime memberDate;

    public Member() {
    }

    public Long getMemberId() { return memberId; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getMemberPw() { return memberPw; }
    public void setMemberPw(String memberPw) { this.memberPw = memberPw; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public LocalDate getMemberBirth() { return memberBirth; }
    public void setMemberBirth(LocalDate memberBirth) { this.memberBirth = memberBirth; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public String getMemberMail() { return memberMail; }
    public void setMemberMail(String memberMail) { this.memberMail = memberMail; }

    public boolean isMemberVip() { return memberVip; }
    public void setMemberVip(boolean memberVip) { this.memberVip = memberVip; }

    public LocalDateTime getMemberDate() { return memberDate; }
    public void setMemberDate(LocalDateTime memberDate) { this.memberDate = memberDate; }
}