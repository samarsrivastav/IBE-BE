package backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class HealthCheckControllerTest {

    @Mock
    private HealthIndicator healthIndicator;

    @InjectMocks
    private HealthCheckController healthCheckController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getHealthStatus_Healthy_ShouldReturnUpStatus() {
        Health expectedHealth = Health.up().build();
        when(healthIndicator.health()).thenReturn(expectedHealth);

        Health actualHealth = healthCheckController.getHealthStatus();

        assertEquals(expectedHealth, actualHealth);
    }

    @Test
    void getHealthStatus_Unhealthy_ShouldReturnDownStatus() {
        Health expectedHealth = Health.down().withDetail("error", "Database down").build();
        when(healthIndicator.health()).thenReturn(expectedHealth);

        Health actualHealth = healthCheckController.getHealthStatus();

        assertEquals(expectedHealth, actualHealth);
    }
}
