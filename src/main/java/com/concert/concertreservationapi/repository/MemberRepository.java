package com.concert.concertreservationapi.repository;


import com.concert.concertreservationapi.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
    boolean existsByMemberMail(String memberMail);

}
