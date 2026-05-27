package com.ticket.fast.gateway.filter;

import com.ticket.fast.common.util.AuthConstant;
import com.ticket.fast.common.util.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider){
        super(Config.class);
        this.jwtProvider = jwtProvider;
    }
    public static class Config {

    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            //  k6 성능 테스트 위함. 헤더에 X-TEST-MODE: true가 포함되어 있으면 인증 로직을 전체 스킵
            String testMode = request.getHeaders().getFirst("X-TEST-MODE");
            if ("true".equals(testMode)) {
                log.info("Performance Test Mode 활성화: 인증 로직을 건너뜁니다. Target: {}", request.getPath());
                // 헤더 청소(remove)를 하지 않고, k6가 보낸 X-USER-ID를 그대로 들고 다음 필터로 넘기기
                return chain.filter(exchange);
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        // 외부에서 조작해서 보낸 ID 헤더가 있다면 여기서 삭제 (보안 강화)
                        httpHeaders.remove(AuthConstant.X_USER_ID);
                        httpHeaders.remove(AuthConstant.X_USER_ROLE);
                    })
                    .build();

            // 토큰 추출
            String authHeader = mutatedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "토큰이 없거나 형식이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 토큰 유효성 검증
            if (!jwtProvider.validateToken(token)) {
                return onError(exchange, "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }

            Claims claims = jwtProvider.getClaims(token);
            String userId = claims.getSubject();
            String role = claims.get("roles", String.class);

            // 검증된 토큰 정보로 헤더 재설정
            ServerHttpRequest finalRequest = mutatedRequest.mutate()
                    .header(AuthConstant.X_USER_ID, userId)
                    .header(AuthConstant.X_USER_ROLE, (role != null) ? role : "")
                    .build();

            log.info("Gateway 인증 통과: User ID {}", userId);

            return chain.filter(exchange.mutate().request(finalRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus){

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(err);
        return response.setComplete();
    }
}
