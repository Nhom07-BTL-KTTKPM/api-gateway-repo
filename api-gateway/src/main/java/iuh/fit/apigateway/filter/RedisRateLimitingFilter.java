package iuh.fit.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RedisRateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${gateway.rate-limit.requests-per-minute:5}")
    private long requestsPerMinute;

    @Value("${gateway.rate-limit.redis-key-prefix:gateway:rate-limit:}")
    private String keyPrefix;

    public RedisRateLimitingFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String identifier = resolveIdentifier(request);
        String redisKey = keyPrefix + identifier;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        if (currentCount != null && currentCount == 1L) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(1));
        }

        if (currentCount != null && currentCount > requestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader(HttpHeaders.RETRY_AFTER, "60");

            Map<String, Object> payload = Map.of(
                    "timestamp", Instant.now().toString(),
                    "success", false,
                    "message", "Too many requests. Please retry after 1 minute.",
                    "error", Map.of(
                            "code", "RATE_LIMIT_EXCEEDED",
                            "detail", "Maximum %d requests per minute exceeded".formatted(requestsPerMinute)
                    )
            );

            response.getWriter().write(objectMapper.writeValueAsString(payload));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String resolveIdentifier(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            return Integer.toHexString(authorizationHeader.hashCode());
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstIp = forwardedFor.split(",")[0].trim();
            if (!firstIp.isBlank()) {
                return firstIp;
            }
        }

        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
