package com.ticket.fast.ticket.aspect;

import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class RoleCheckAspect {

    @Around("@annotation(com.ticket.fast.common.annotation.AdminOnly)")
    public Object checkAdminRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 메서드 파라미터 중 AuthUser를 찾습니다.
        Object[] args = joinPoint.getArgs();
        AuthUser authUser = Arrays.stream(args)
                .filter(arg -> arg instanceof AuthUser)
                .map(arg -> (AuthUser) arg)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_ADMIRE_ACTION));

        // 2. 관리자 권한이 없으면 에러 Mono를 반환합니다.
        if (!"ADMIN".equals(authUser.role())) {
            log.warn("권한 부족: userId={}, role={}", authUser.userId(), authUser.role());

            return Mono.error(new BusinessException(ErrorCode.ADMIN_ADMIRE_ACTION));
        }

        // 3. 관리자라면 원래 메서드를 실행합니다.
        return joinPoint.proceed();
    }
}
