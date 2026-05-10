package com.ticket.fast.member.controller;

import com.ticket.fast.common.annotation.LoginUser;
import com.ticket.fast.common.dto.ApiResponse;

import com.ticket.fast.common.dto.TokenResponse;
import com.ticket.fast.member.dto.request.MemberJoinRequest;
import com.ticket.fast.member.dto.request.MemberLoginRequest;
import com.ticket.fast.member.dto.response.MemberResponse;
import com.ticket.fast.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/member")
@RequiredArgsConstructor
@RestController
public class MemberController {

    private final MemberService memberService;

    //
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> joinMember(@Valid @RequestBody MemberJoinRequest request){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberService.joinMember(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody MemberLoginRequest request){
        return ResponseEntity.ok(ApiResponse.success(memberService.login(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(@LoginUser Long userId){
        return ResponseEntity.ok(ApiResponse.success(memberService.getMyInfo(userId)));
    }

}
