package iuh.fit.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GatewayCorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public GatewayCorsConfig(@Value("${gateway.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOriginsCsv) {
        this.allowedOrigins = StringUtils.commaDelimitedListToStringArray(allowedOriginsCsv);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
