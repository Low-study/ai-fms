package com.aifms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux configuration.
 * Explicitly enables reactive web support.
 */
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    // Additional WebFlux customization goes here if needed.
}
