package com.concert.concertreservationapi.dto;

public class MemberRequest {

    private String loginId;
    private String memberPw;
    private String memberName;
    private String memberMail;

    public MemberRequest() {

    }

    public String getLoginId() {return loginId;}
    public void setLoginId(String loginId) {this.loginId = loginId;}

    public String getMemberPw() {return memberPw;}
    public void setMemberPw(String memberPw) {this.memberPw = memberPw;}

    public String getMemberName() {return memberName;}
    public void setMemberName(String memberName) {this.memberName = memberName;}

    public String getMemberMail() {return memberMail;}
    public void setMemberMail(String memberMail) {this.memberMail = memberMail;}

}
