package com.aifms.modules.auth.presentation;

import com.aifms.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Health check controller.
 * Verifies the application is running.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Mono<Result<String>> health() {
        return Mono.just(Result.success("OK"));
    }
}
