package iuh.fit.apigateway.controller;

import iuh.fit.shared.api.ApiError;
import iuh.fit.shared.api.ApiResponse;
import iuh.fit.shared.trace.TraceIdConstants;
import iuh.fit.shared.trace.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class GatewayFallbackController {

        @RequestMapping("/{service}")
        public ResponseEntity<ApiResponse<Void>> fallback(@PathVariable String service, HttpServletRequest request) {
                log.warn("Circuit breaker fallback triggered for service: {}", service);

                ApiError error = new ApiError(
                                "GATEWAY_FALLBACK",
                                "Circuit breaker fallback from gateway",
                                Map.of("service", service),
                                null
                );

                ApiResponse<Void> payload = ApiResponse.failure(
                                "Service temporarily unavailable",
                                error,
                                resolveTraceId(request)
                );

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(payload);
        }

        private static String resolveTraceId(HttpServletRequest request) {
                if (request != null) {
                        Object attr = request.getAttribute(TraceIdConstants.REQUEST_ATTRIBUTE);
                        if (attr instanceof String traceId && !traceId.isBlank()) {
                                return traceId;
                        }

                        String headerTraceId = request.getHeader(TraceIdConstants.HEADER_NAME);
                        if (headerTraceId != null && !headerTraceId.isBlank()) {
                                return headerTraceId;
                        }
                }

                String contextTraceId = TraceIdContext.get();
                return (contextTraceId == null || contextTraceId.isBlank()) ? null : contextTraceId;
        }
}

