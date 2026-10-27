package com.concert.concertreservationapi.dto;

public class LoginRequest {

    private String loginId;
    private String memberPw;

    public LoginRequest() {
    }

    public String getLoginId() {return loginId;}
    public void setLoginId(String loginId) {this.loginId = loginId;}

    public String getMemberPw() {return memberPw;}
    public void setMemberPw(String memberPw) {this.memberPw = memberPw;}
}
