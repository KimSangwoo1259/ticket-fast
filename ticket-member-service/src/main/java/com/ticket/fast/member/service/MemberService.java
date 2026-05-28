package com.ticket.fast.member.service;

import com.ticket.fast.common.dto.TokenResponse;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import com.ticket.fast.common.util.JwtProvider;
import com.ticket.fast.member.domain.Member;
import com.ticket.fast.member.dto.request.MemberJoinRequest;
import com.ticket.fast.member.dto.request.MemberLoginRequest;
import com.ticket.fast.member.dto.response.MemberResponse;
import com.ticket.fast.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public MemberResponse joinMember(MemberJoinRequest request){

        boolean exists = memberRepository.existsByEmail(request.email());

        // 이메일 중복 체크
        if (exists){
            log.warn("이미 존재 하는 이메일로 가입 시도 email {}", request.email());
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        Member savedMember = memberRepository.save(member);
        log.info("새로운 회원 가입 성공 id {}, email {}",member.getId(),member.getEmail());
        return MemberResponse.fromEntity(savedMember);
    }

    public TokenResponse login(MemberLoginRequest request){
        // 회원 존재 여부 확인
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("로그인 실패: 존재하지 않는 이메일 - {}", request.email());
                    return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                });

        // 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            log.warn("로그인 실패: 비밀번호 불일치 - {}", request.email());
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 권한 정보 추출 (MemberRole -> String 변환)
        String roles = member.getMemberRoles().stream()
                .map(memberRole -> memberRole.getRole().getRoleName())
                .collect(Collectors.joining(","));

        // 4. 토큰 생성 및 반환
        return jwtProvider.createToken(member.getId(), member.getEmail(), roles);
    }

    public MemberResponse getMyInfo(Long userId){
        Member member = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        log.info("회원 정보 조회 userId {}", userId);

        return MemberResponse.fromEntity(member);
    }
}
