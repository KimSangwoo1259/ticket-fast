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
        Object[] args = joinPoint.getArgs();
        AuthUser authUser = Arrays.stream(args)
                .filter(arg -> arg instanceof AuthUser)
                .map(arg -> (AuthUser) arg)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_ADMIRE_ACTION));

        if (!"ADMIN".equals(authUser.role())) {
            log.warn("권한 부족: userId={}, role={}", authUser.userId(), authUser.role());

            return Mono.error(new BusinessException(ErrorCode.ADMIN_ADMIRE_ACTION));
        }

        return joinPoint.proceed();
    }
}
