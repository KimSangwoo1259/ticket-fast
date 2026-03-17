package com.ticket.fast.member.dto.response;

import com.ticket.fast.member.domain.Member;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String name,
        LocalDateTime createdAt
) {

    public static MemberResponse fromEntity(Member member){
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getCreatedAt()
        );
    }
}
