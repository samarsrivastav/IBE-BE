package backend.service;

import backend.entity.Booking;
import backend.entity.Room;
import backend.entity.enums.CleaningType;
import backend.entity.enums.Shift;
import backend.model.BookingTransaction;
import backend.model.RoomCleaningSchedule;
import backend.repository.BookingTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final PDFService pdfService;
    private final BookingTransactionRepository bookingTransactionRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${application.name:Hotel Booking System}")
    private String appName;

    @Value("${admin.email:guptamanan24@gmail.com}")
    private String adminEmail;

    // Existing OTP email functionality
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject("Your Verification Code for Purchase");

            String emailContent = buildOtpEmail(otp);
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Could not send OTP email", e);
        }
    }

    // New booking confirmation functionality
    public void sendBookingConfirmation(String email , BookingTransaction transaction) {
        try {
            log.info("Preparing booking confirmation email for: {}", email);

            // Generate PDF invoice
            byte[] pdfInvoice = pdfService.generateBookingInvoice(transaction);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(email);
            helper.setSubject("Booking Confirmation - Details");

            String emailContent = buildBookingConfirmationEmail(transaction);
            helper.setText(emailContent, true);

            // Attach PDF invoice
            helper.addAttachment("booking_invoice_" + transaction.getConfirmationId() + ".pdf",
                    new ByteArrayResource(pdfInvoice));

            mailSender.send(mimeMessage);
            log.info("Booking confirmation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to: {}", email, e);
            throw new RuntimeException("Could not send booking confirmation email", e);
        }
    }

    private String buildOtpEmail(String otp) {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        String messageId = "OTP-" + System.currentTimeMillis();

        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>"
                + "<h2 style='color: #3498db;'>Hello!</h2>"
                + "<p>We received a request to verify your purchase on <strong>" + timestamp + "</strong>.</p>"
                + "<p>Your One-Time Password (OTP) is:</p>"
                + "<div style='font-size: 36px; font-weight: bold; letter-spacing: 4px; text-align: center; margin: 30px 0; color: #2c3e50; background-color: #f1f1f1; padding: 15px; border-radius: 8px;'>"
                + otp + "</div>"
                + "<p>This code will expire in 5 minutes. Please do not share it with anyone.</p>"
                + "<p>If you did not initiate this request, you can safely ignore this email.</p>"
                + "<p style='margin-top: 30px;'>Thank you,<br/><strong>" + appName + " Team</strong></p>"
                + "<hr style='border: none; border-top: 1px solid #ccc; margin-top: 40px;'/>"
                + "<p style='font-size: 12px; color: #888;'>This is an automated message. Please do not reply to this email.</p>"
                + "<p style='display: none;'>Message ID: " + messageId + "</p>"
                + "</div>";
    }

    private String buildBookingConfirmationEmail(BookingTransaction transaction) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

        try {
            // Convert JsonNode to Map using ObjectMapper
            Map<String, Object> bookingDetails = objectMapper.convertValue(transaction.getBookingDetails(), Map.class);
            Map<String, Object> confirmationDetails = (Map<String, Object>) bookingDetails.get("confirmationDetails");
            Map<String, Object> travelerInfo = (Map<String, Object>) bookingDetails.get("travelerInfo");

            // Extract necessary details
            String guestName = travelerInfo.get("firstName") + " " + travelerInfo.get("lastName");
            String roomName = (String) confirmationDetails.get("roomName");
            int roomCount = (int) confirmationDetails.get("roomCount");
            String checkInDate = LocalDate.parse((String) confirmationDetails.get("startDate")).format(dateFormatter);
            String checkOutDate = LocalDate.parse((String) confirmationDetails.get("endDate")).format(dateFormatter);
            double totalCost = (double) confirmationDetails.get("totalCost");
            int adults = (int) confirmationDetails.get("adultCount");
            int children = (int) confirmationDetails.get("childCount");

            // Build HTML email
            return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 10px;'>"
                    + "<h2 style='color: #2c3e50;'>Booking Confirmation</h2>"
                    + "<p style='font-size: 16px;'>Hi <strong>" + guestName + "</strong>,</p>"
                    + "<p style='font-size: 16px;'>Thank you for your reservation! Your booking is confirmed.</p>"

                    + "<div style='background-color: #f8f9fa; padding: 16px; border-radius: 8px; margin-top: 20px;'>"
                    + "<h3 style='color: #3498db;'>Reservation Details</h3>"
                    + "<p><strong>Booking ID:</strong> " + transaction.getConfirmationId() + "</p>"

                    + "<p><strong>Room Type:</strong> " + roomName + "</p>"
                    + "<p><strong>Room(s):</strong> " + roomCount + "</p>"
                    + "<p><strong>Guests:</strong> " + adults + " Adults, " + children + " Children</p>"
                    + "<p><strong>Check-in:</strong> " + checkInDate + "</p>"
                    + "<p><strong>Check-out:</strong> " + checkOutDate + "</p>"
                    + "<p><strong>Total Cost:</strong> " + currencyFormatter.format(totalCost) + "</p>"
                    + "</div>"

                    + "<p style='margin-top: 20px;'>We've attached your invoice as a PDF to this email. Please keep this email for your records.</p>"
                    + "<p>We look forward to hosting you!</p>"
                    + "<p>Warm regards,<br/>" + appName + " Team</p>"
                    + "<p style='font-size: 12px; color: #999; margin-top: 20px;'>This is an automated email. Please do not reply.</p>"
                    + "</div>";
        } catch (Exception e) {
            log.error("Error building booking confirmation email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build booking confirmation email", e);
        }
    }

    /**
     * Send an email to the admin
     * @param subject The email subject
     * @param body The email body
     */
    public void sendEmailToAdmin(String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(adminEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(mimeMessage);
            log.info("Admin email sent successfully with subject: {}", subject);
        } catch (Exception e) {
            log.error("Failed to send admin email: {}", e.getMessage(), e);
        }
    }

    /**
     * Send a critical staff shortage alert email to the admin
     * @param date The date of the shortage
     * @param shift The shift with the shortage
     * @param requiredStaffCount The number of staff required
     * @param pendingSchedules The list of pending room cleaning schedules
     * @param shiftDurationHours The duration of the shift in hours
     * @param dailyCleaningDuration The duration for daily cleaning in minutes
     * @param deepCleaningDuration The duration for deep cleaning in minutes
     * @param shiftStartTime The start time of the shift
     * @param shiftEndTime The end time of the shift
     */
    public void sendCriticalStaffShortageAlert(LocalDate date, Shift shift, int requiredStaffCount,
                                               List<RoomCleaningSchedule> pendingSchedules,
                                               int shiftDurationHours,
                                               int dailyCleaningDuration,
                                               int deepCleaningDuration,
                                               LocalTime shiftStartTime,
                                               LocalTime shiftEndTime) {
        try {
            // Build email content with all relevant details
            String subject = "CRITICAL STAFF SHORTAGE ALERT: " + shift + " Shift on " + date;

            StringBuilder body = new StringBuilder();
            body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>");
            body.append("<h2 style='color: #e74c3c;'>Critical Staff Shortage Detected</h2>");

            body.append("<div style='background-color: #f8f9fa; padding: 16px; border-radius: 8px; margin-top: 20px;'>");
            body.append("<h3 style='color: #e74c3c;'>Shortage Details</h3>");
            body.append("<p><strong>Date:</strong> ").append(date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))).append("</p>");
            body.append("<p><strong>Shift:</strong> ").append(shift).append("</p>");
            body.append("<p><strong>Required Staff:</strong> ").append(requiredStaffCount).append("</p>");
            body.append("<p><strong>Available Staff:</strong> 0</p>");
            body.append("<p><strong>Shift Hours:</strong> ").append(shiftStartTime).append(" to ").append(shiftEndTime).append("</p>");
            body.append("</div>");

            // Add details about pending schedules
            body.append("<div style='background-color: #f8f9fa; padding: 16px; border-radius: 8px; margin-top: 20px;'>");
            body.append("<h3 style='color: #3498db;'>Pending Cleaning Schedules</h3>");

            // Count room types
            int deepCleaningRooms = 0;
            int dailyCleaningRooms = 0;

            for (RoomCleaningSchedule schedule : pendingSchedules) {
                if (schedule.getCleaningType() == CleaningType.DEEP_CLEANING) {
                    deepCleaningRooms++;
                } else {
                    dailyCleaningRooms++;
                }
            }

            body.append("<p><strong>Total Rooms:</strong> ").append(pendingSchedules.size()).append("</p>");
            body.append("<p><strong>Deep Cleaning Rooms:</strong> ").append(deepCleaningRooms).append("</p>");
            body.append("<p><strong>Daily Cleaning Rooms:</strong> ").append(dailyCleaningRooms).append("</p>");
            body.append("</div>");

            // Add configuration parameters
            body.append("<div style='background-color: #f8f9fa; padding: 16px; border-radius: 8px; margin-top: 20px;'>");
            body.append("<h3 style='color: #3498db;'>Configuration Parameters</h3>");
            body.append("<p><strong>Shift Duration:</strong> ").append(shiftDurationHours).append(" hours</p>");
            body.append("<p><strong>Daily Cleaning Duration:</strong> ").append(dailyCleaningDuration).append(" minutes</p>");
            body.append("<p><strong>Deep Cleaning Duration:</strong> ").append(deepCleaningDuration).append(" minutes</p>");

            // Calculate total cleaning time required
            int totalMinutesRequired = (dailyCleaningRooms * dailyCleaningDuration) +
                    (deepCleaningRooms * deepCleaningDuration);
            int totalHoursRequired = totalMinutesRequired / 60;
            int remainingMinutes = totalMinutesRequired % 60;

            body.append("<p><strong>Total Cleaning Time Required:</strong> ").append(totalHoursRequired)
                    .append(" hours and ").append(remainingMinutes).append(" minutes</p>");
            body.append("</div>");

            body.append("<div style='margin-top: 20px; padding: 15px; background-color: #f8d7da; border-radius: 5px; color: #721c24;'>");
            body.append("<p><strong>URGENT ACTION REQUIRED:</strong> Please assign staff to this shift immediately.</p>");
            body.append("</div>");

            body.append("<p style='margin-top: 30px;'>This is an automated message from the Room Cleaning Management System.</p>");
            body.append("</div>");

            // Use the existing method to send the email to admin
            sendEmailToAdmin(subject, body.toString());

            log.info("Critical staff shortage email notification sent to admin");
        } catch (Exception e) {
            log.error("Failed to send critical staff shortage email notification", e);
        }
    }
}