package com.lifeos.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.common.response.Result;
import com.lifeos.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.annotation.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 白名单路径，不需要登录即可访问
    private static final List<String> whiteList = new ArrayList<>();

    static {
        whiteList.add("/api/user/login");
        whiteList.add("/api/user/register");
        whiteList.add("/swagger-ui.html");
        whiteList.add("/swagger-ui/**");
        whiteList.add("/v3/api-docs/**");
        whiteList.add("/service-docs/**");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        log.info("Gateway Filter: Path=" + path);

        // 1. 检查是否在白名单中
        for (String whitePath : whiteList) {
            if (antPathMatcher.match(whitePath, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. 获取Token
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            return authFailed(response, "Missing Authorization header");
        }

        // 3. 解析和验证 Token
        String jwtToken = JwtUtil.extractToken(token);
        Claims claims = JwtUtil.parseToken(jwtToken);

        if (claims == null) {
            return authFailed(response, "Invalid or expired token");
        }

        Long userId = Long.parseLong(claims.getSubject());

        // 4. 从 Redis 校验 Token 是否有效
        String redisToken = stringRedisTemplate.opsForValue().get("token:" + userId);
        if (redisToken == null || !redisToken.equals(jwtToken)) {
            return authFailed(response, "Token expired or logged out");
        }

        // 5. 将 userId 放入请求头中传递给下游微服务
        ServerHttpRequest newRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Name", claims.get("username", String.class))
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    private Mono<Void> authFailed(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        Result<Object> result = Result.error(401, message);
        String resultString;
        try {
            resultString = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            resultString = "{\"code\":401,\"message\":\"" + message + "\"}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(resultString.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // 优先级最高
    }
}
