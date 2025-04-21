package backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HealthNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(HealthNotificationService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Value("${admin.email:}")
    private String adminEmail;

    // Map to track service status changes to avoid sending repeated notifications
    private final Map<String, Status> previousServiceStatus = new ConcurrentHashMap<>();

    /**
     * Sends alert notifications for down services
     *
     * @param servicesStatus Map of service names to their statuses
     * @param downServicesMessage Message listing which services are down
     */
    public void sendServiceDownAlert(Map<String, Status> servicesStatus, String downServicesMessage) {
        if (adminEmail == null || adminEmail.isEmpty()) {
            logger.warn("Admin email not configured. Cannot send service down alert.");
            return;
        }

        // Check if there are status changes to avoid spamming alerts
        boolean statusChanged = false;
        Map<String, Status> changedServices = new HashMap<>();

        for (Map.Entry<String, Status> entry : servicesStatus.entrySet()) {
            String serviceName = entry.getKey();
            Status currentStatus = entry.getValue();
            Status previousStatus = previousServiceStatus.get(serviceName);

            // Send notification if:
            // 1. Service is DOWN and wasn't tracked before
            // 2. Service is DOWN and was previously UP
            if (Status.DOWN.equals(currentStatus) &&
                    (previousStatus == null || Status.UP.equals(previousStatus))) {
                statusChanged = true;
                changedServices.put(serviceName, currentStatus);
            }

            // Update previous status
            previousServiceStatus.put(serviceName, currentStatus);
        }

        // Send alert only if there's a status change
        if (statusChanged) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("⚠️ SERVICE ALERT: Services are DOWN");
            message.setText("The following services are currently down:\n\n" +
                    downServicesMessage +
                    "\n\nPlease investigate immediately.");

            try {
                emailSender.send(message);
                logger.info("Service down alert sent to: {}", adminEmail);
            } catch (Exception e) {
                logger.error("Failed to send service down alert: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Send notification when a service recovers from a down state
     *
     * @param serviceName The name of the service that recovered
     */
    public void sendServiceRecoveryAlert(String serviceName) {
        if (adminEmail == null || adminEmail.isEmpty()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("✅ SERVICE RECOVERED: " + serviceName);
        message.setText("The service " + serviceName + " has recovered and is now operational.\n\n" +
                "No further action is required.");

        try {
            emailSender.send(message);
            logger.info("Service recovery alert sent for: {}", serviceName);
        } catch (Exception e) {
            logger.error("Failed to send service recovery alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to check and reset notification status for services that have been fixed
     * but weren't detected by the system
     */
    @Scheduled(fixedRate = 86400000) // Run once a day
    public void resetNotificationStatus() {
        logger.info("Resetting notification status for all services");
        previousServiceStatus.clear();
    }
}