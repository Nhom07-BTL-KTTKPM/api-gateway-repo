package iuh.fit.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class GatewayFallbackController {

    @GetMapping("/{service}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service) {
        log.warn("Circuit breaker fallback triggered for service: {}", service);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "success", false,
                        "message", "Service temporarily unavailable",
                        "service", service,
                        "error", Map.of(
                                "code", "GATEWAY_FALLBACK",
                                "detail", "Circuit breaker fallback from gateway"
                        )
                ));
    }
}
