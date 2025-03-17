package backend.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    private final HealthIndicator healthIndicator;

    public HealthCheckController(@Qualifier("customHealthIndicator") HealthIndicator healthIndicator) {
        this.healthIndicator = healthIndicator;
    }

    @GetMapping("/health")
    public Health getHealthStatus() {
        return healthIndicator.health();
        //hello

    }
}
