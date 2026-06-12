package com.KIRA_ZINA.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // All CORS logic is handled by RateLimitFilter at the servlet filter level.
    // CORS is intentionally removed from Spring MVC to prevent duplicate header
    // injection, which causes browsers to reject otherwise valid 200 OK responses.
}
