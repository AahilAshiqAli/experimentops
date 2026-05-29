package com.experimentops.gateway.filter;

import com.experimentops.gateway.model.dto.JwtDto;
import com.experimentops.gateway.util.JwtUtil;
import com.experimentops.utils.constant.Headers;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.experimentops.utils.constant.Headers.X_TOKEN_C_USER_NAME;
import static com.experimentops.utils.constant.Headers.X_TOKEN_C_USER_ROLE;
import static com.experimentops.utils.constant.Headers.X_TOKEN_C_USER_UUID;
import static com.experimentops.utils.constant.Headers.X_TOKEN_C_WORKSPACE_UUID;

//Gateway needs to handle many concurrent proxied requests efficiently. Spring Cloud Gateway was designed around Netty/WebFlux, not servlet containers like Tomcat.
//Backend services are normal request-response business APIs, usually running on Tomcat/Servlet.
@RequiredArgsConstructor
@Component
public class JWTFilter implements GlobalFilter, Ordered {
    private static final List<String> INTERNAL_IDENTITY_HEADERS = List.of(
            X_TOKEN_C_USER_NAME,
            X_TOKEN_C_WORKSPACE_UUID,
            X_TOKEN_C_USER_UUID,
            X_TOKEN_C_USER_ROLE
    );

    private final JwtUtil jwtUtil;

    @Override
    public int getOrder() {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 6;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest oldRequest = exchange.getRequest();
        String authorization = oldRequest.getHeaders().getFirst(Headers.AUTHORIZATION);
        if (StringUtils.isBlank(authorization)) {
            return filterNoAuth(oldRequest, exchange, chain);
        }

        JwtDto jwtDto = jwtUtil.parseAuthorization(authorization);
        if (jwtDto == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return filterAuth(oldRequest, jwtDto, exchange, chain);
    }

    private Mono<Void> filterNoAuth(ServerHttpRequest oldRequest, ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = oldRequest.mutate()
                .headers(headers -> INTERNAL_IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> filterAuth(ServerHttpRequest oldRequest, JwtDto jwtDto,
                                  ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder requestBuilder = oldRequest.mutate();
        addHeader(requestBuilder, X_TOKEN_C_USER_NAME, jwtDto.getClaim().getName());
        addHeader(requestBuilder, X_TOKEN_C_WORKSPACE_UUID, jwtDto.getClaim().getWorkspaceUuid());
        addHeader(requestBuilder, X_TOKEN_C_USER_UUID, jwtDto.getClaim().getUserUuid());
        addHeader(requestBuilder, X_TOKEN_C_USER_ROLE, jwtDto.getClaim().getRole());

        if (StringUtils.isBlank(jwtDto.getClaim().getWorkspaceUuid())
                && StringUtils.equalsIgnoreCase(jwtDto.getClaim().getRole(), "PLATFORM_ADMIN")) {
            addHeader(requestBuilder, X_TOKEN_C_WORKSPACE_UUID, "platform");
        }

        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    private void addHeader(ServerHttpRequest.Builder requestBuilder, String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            requestBuilder.headers(headers -> {
                headers.remove(name);
                headers.add(name, value);
            });
        }
    }
}
