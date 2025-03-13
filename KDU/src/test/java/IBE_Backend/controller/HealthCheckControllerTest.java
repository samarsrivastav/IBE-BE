package IBE_Backend.controller;

import IBE_Backend.health.CustomHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthCheckController.class)
public class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "customHealthIndicator")
    private CustomHealthIndicator healthIndicator;

    @Test
    void testHealthEndpoint_WhenUp() throws Exception {
        when(healthIndicator.health()).thenReturn(
                Health.up().withDetail("message", "Service is running smoothly").build()
        );

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.message").value("Service is running smoothly"));
    }

    @Test
    void testHealthEndpoint_WhenDown() throws Exception {
        when(healthIndicator.health()).thenReturn(
                Health.down().withDetail("message", "Service is experiencing issues").build()
        );

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk()) // Note: The status is still 200 OK even when health is DOWN
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.details.message").value("Service is experiencing issues"));
    }
}