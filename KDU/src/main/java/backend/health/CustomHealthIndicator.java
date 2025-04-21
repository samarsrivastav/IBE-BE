package backend.health;

import backend.service.EmailHealthService;
import backend.service.EmailService;
import backend.service.HealthNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private HealthNotificationService notificationService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailHealthService emailHealthService;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${graphql.endpoint:}")
    private String graphqlEndpoint;

    @Value("${graphql.apiKey:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Map<String, Status> serviceStatus = new HashMap<>();

        // Check database health
        boolean dbHealthy = checkDatabaseHealth();
        serviceStatus.put("database", dbHealthy ? Status.UP : Status.DOWN);

        // Check GraphQL API health
        boolean graphqlHealthy = checkGraphQLHealth();
        serviceStatus.put("graphqlAPI", graphqlHealthy ? Status.UP : Status.DOWN);

        // Add more third-party API health checks here

        // Email Service health
        boolean emailServiceHealthy = checkEmailServiceHealth();
        serviceStatus.put("emailService", emailServiceHealthy ? Status.UP : Status.DOWN);

        // Collect down services
        StringBuilder downServices = new StringBuilder();
        for (Map.Entry<String, Status> service : serviceStatus.entrySet()) {
            if (Status.DOWN.equals(service.getValue())) {
                if (downServices.length() > 0) {
                    downServices.append(", ");
                }
                downServices.append(service.getKey());
            }
        }

        // Build the health response
        details.put("services", serviceStatus);

        if (downServices.length() > 0) {
            String downServicesMsg = downServices.toString();
            details.put("downServices", downServicesMsg);
            details.put("message", "The following services are down: " + downServicesMsg);

            // Send notification about down services
            notificationService.sendServiceDownAlert(serviceStatus, downServicesMsg);

            return Health.down().withDetails(details).build();
        } else {
            details.put("message", "All services are running smoothly.");
            return Health.up().withDetails(details).build();
        }
    }

    private boolean checkDatabaseHealth() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            int result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkGraphQLHealth() {

        if (graphqlEndpoint == null || graphqlEndpoint.isEmpty()) {
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key", apiKey);

            // Simple introspection query to check GraphQL endpoint health
            String query = "{\"query\":\"{ __schema { queryType { name } } }\"}";

            HttpEntity<String> entity = new HttpEntity<>(query, headers);
            restTemplate.exchange(graphqlEndpoint, HttpMethod.POST, entity, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private boolean checkEmailServiceHealth() {
        // Use the dedicated EmailHealthService to check email service health
        try {
            return emailHealthService.isEmailServiceHealthy();
        } catch (Exception e) {
            return false;
        }
    }
}
