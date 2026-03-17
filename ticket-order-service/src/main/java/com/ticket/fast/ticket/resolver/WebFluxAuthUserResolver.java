package com.ticket.fast.ticket.resolver;

import com.ticket.fast.common.annotation.LoginUser;
import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.util.AuthConstant;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class WebFluxAuthUserResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class) &&
                parameter.getParameterType().equals(AuthUser.class);
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(AuthConstant.X_USER_ID);
        String role = exchange.getRequest().getHeaders().getFirst(AuthConstant.X_USER_ROLE);

        if (!StringUtils.hasText(userId)){
            return Mono.empty();
        }

        return Mono.just(new AuthUser(Long.valueOf(userId), role));
    }
}
