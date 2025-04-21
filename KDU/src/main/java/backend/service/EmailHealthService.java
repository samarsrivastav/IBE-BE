package backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.net.Socket;
import java.util.Properties;

@Service
@Slf4j
public class EmailHealthService {

    private static final Logger logger = LoggerFactory.getLogger(EmailHealthService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:25}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    /**
     * Checks if email service is properly configured and the mail server is reachable
     * without actually sending an email
     *
     * @return true if the email service is healthy, false otherwise
     */
    public boolean isEmailServiceHealthy() {
        // First, check if configuration is present
        if (mailHost == null || mailHost.isEmpty() ||
                mailUsername == null || mailUsername.isEmpty()) {
            logger.warn("Email service configuration is incomplete - missing host or username");
            return false;
        }

        // Next, check if JavaMailSender is available and properly configured
        if (mailSender == null) {
            logger.warn("JavaMailSender is not available");
            return false;
        }

        // First try: Socket connection to mail server
        try {
            logger.debug("Attempting socket connection to mail server: {}:{}", mailHost, mailPort);
            try (Socket socket = new Socket(mailHost, mailPort)) {
                // If we can open a socket to the mail server, it's reachable
                logger.debug("Successfully established socket connection to mail server");
                return true;
            }
        } catch (Exception e) {
            logger.warn("Could not establish socket connection to mail server: {}", e.getMessage());

            // Second try: If socket connection failed, try SMTP server connection
            if (mailSender instanceof JavaMailSenderImpl) {
                try {
                    JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;

                    // Get properties from the mail sender
                    Properties props = new Properties();
                    props.putAll(mailSenderImpl.getJavaMailProperties());

                    // Create a mail session
                    Session session = Session.getInstance(props);

                    // Try to connect to the SMTP server
                    Transport transport = session.getTransport("smtp");
                    transport.connect(mailHost, mailPort, mailUsername, mailPassword);
                    transport.close();

                    logger.debug("Successfully connected to SMTP server");
                    return true;
                } catch (MessagingException e2) {
                    logger.warn("Mail server connection test failed: {}", e2.getMessage());
                }
            }
        }

        // If all tests failed, email service is not healthy
        return false;
    }
}