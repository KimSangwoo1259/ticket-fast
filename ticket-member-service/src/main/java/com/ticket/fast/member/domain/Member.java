package com.ticket.fast.member.domain;

import com.ticket.fast.common.domain.BaseEntity;
import com.ticket.fast.common.util.TsidUtil;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MemberRole> memberRoles = new ArrayList<>();

    @Builder
    private Member(String email, String password, String name){
        this.id = TsidUtil.nextLong();
        this.email = email;
        this.password = password;
        this.name = name;
    }
}
