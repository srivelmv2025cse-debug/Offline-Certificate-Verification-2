package com.certificateverification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the Certificate Verification application.
 * Configures static resource handlers and other MVC settings.
 * Day 1: Basic configuration - security config added in Day 2+.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure static resource serving from the standard locations.
     * Ensures CSS, JS, and image files are served correctly.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
