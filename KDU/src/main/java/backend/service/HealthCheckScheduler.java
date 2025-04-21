//package backend.service;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.actuate.health.Health;
//import org.springframework.boot.actuate.health.HealthEndpoint;
//import org.springframework.boot.actuate.health.Status;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
//@Component
//public class HealthCheckScheduler {
//
//    private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);
//
//    @Autowired
//    private HealthEndpoint healthEndpoint;
//
//    /**
//     * Scheduled task that runs every 5 minutes to check the health of all services
//     * The health check details are automatically logged and any down services trigger notifications
//     * via the CustomHealthIndicator's integration with HealthNotificationService
//     */
//    @Scheduled(fixedRate = 300000) // Every 5 minutes
//    public void checkServicesHealth() {
//        logger.info("Running scheduled health check...");
//
//        try {
//            // This will trigger the CustomHealthIndicator which handles notifications
//            Health health = (Health) healthEndpoint.health();
//
//            if (Status.UP.equals(health.getStatus())) {
//                logger.info("All services are healthy");
//            } else {
//                Map<String, Object> details = health.getDetails();
//                String downServices = (String) details.get("downServices");
//                logger.warn("Health check detected down services: {}", downServices);
//            }
//        } catch (Exception e) {
//            logger.error("Error during scheduled health check: {}", e.getMessage(), e);
//        }
//    }
//}