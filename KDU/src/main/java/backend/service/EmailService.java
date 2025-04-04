package backend.service;

import backend.entity.Booking;
import backend.entity.Room;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final PDFService pdfService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${application.name:Hotel Booking System}")
    private String appName;

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

    public void sendBookingConfirmation(String toEmail) {
        try {
            // Create sample booking data
            Booking sampleBooking = createSampleBooking();

            // Generate sample PDF invoice
            byte[] pdfInvoice = pdfService.generateBookingInvoice(sampleBooking);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject("Booking Confirmation - #" + sampleBooking.getBookingReference());

            String emailContent = buildBookingConfirmationEmail(sampleBooking);
            helper.setText(emailContent, true);

            // Attach PDF invoice
            helper.addAttachment("booking_invoice_" + sampleBooking.getBookingReference() + ".pdf",
                    new ByteArrayResource(pdfInvoice));

            mailSender.send(mimeMessage);
            log.info("Booking confirmation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to: {}", toEmail, e);
            throw new RuntimeException("Could not send booking confirmation email", e);
        }
    }

    private String buildOtpEmail(String otp) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>"
                + "<h2 style='color: #3498db;'>Purchase Verification</h2>"
                + "<p>Please use the following code to complete your purchase:</p>"
                + "<h1 style='font-size: 32px; letter-spacing: 2px; text-align: center; margin: 30px 0; color: #2c3e50; background-color: #f8f9fa; padding: 10px; border-radius: 5px;'>" + otp + "</h1>"
                + "<p>This code is valid for 5 minutes.</p>"
                + "<p>If you did not request this code, please ignore this email.</p>"
                + "<p>Thank you,<br/>" + appName + " Team</p>"
                + "<p style='font-size: 12px; color: #777; margin-top: 20px;'>This is an automated message, please do not reply to this email.</p>"
                + "</div>";
    }

    private String buildBookingConfirmationEmail(Booking booking) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

        StringBuilder roomDetailsBuilder = new StringBuilder();
        for (int i = 0; i < booking.getRooms().size(); i++) {
            Room room = booking.getRooms().get(i);
            roomDetailsBuilder.append("<p><strong>Room ").append(i+1).append(":</strong> ")
                    .append(room.getRoomType()).append(" - ")
                    .append(currencyFormatter.format(room.getPrice())).append(" per night</p>");
        }

        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>"
                + "<h2 style='color: #3498db;'>Booking Confirmation</h2>"
                + "<p>Thank you for your booking. Your reservation is confirmed!</p>"
                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>"
                + "<h3>Booking Details</h3>"
                + "<p><strong>Booking Reference:</strong> " + booking.getBookingReference() + "</p>"
                + "<p><strong>Check-in Date:</strong> " + booking.getCheckInDate().format(dateFormatter) + "</p>"
                + "<p><strong>Check-out Date:</strong> " + booking.getCheckOutDate().format(dateFormatter) + "</p>"
                + "<h4>Guests</h4>"
                + "<p>Adults: " + booking.getAdultsCount() + "</p>"
                + "<p>Children: " + booking.getChildrenCount() + "</p>"
                + "<h4>Room Details</h4>"
                + roomDetailsBuilder.toString()
                + "<h4>Payment Summary</h4>"
                + "<p><strong>Total Amount:</strong> " + currencyFormatter.format(booking.getTotalAmount()) + "</p>"
                + "</div>"
                + "<p>We have attached your invoice as a PDF to this email.</p>"
                + "<p>We look forward to welcoming you!</p>"
                + "<p>Thank you,<br/>" + appName + " Team</p>"
                + "<p style='font-size: 12px; color: #777; margin-top: 20px;'>Please keep this email for your records.</p>"
                + "</div>";
    }

    // Helper method to create sample booking data
    private Booking createSampleBooking() {
        // Create sample rooms
        Room room1 = new Room();
        room1.setRoomType("Deluxe King");
        room1.setPrice(199.99);

        Room room2 = new Room();
        room2.setRoomType("Superior Suite");
        room2.setPrice(299.99);

        List<Room> rooms = new ArrayList<>(Arrays.asList(room1, room2));

        // Create sample booking
        Booking booking = new Booking();
        booking.setBookingReference("BK" + System.currentTimeMillis());
        booking.setCheckInDate(LocalDate.now().plusDays(30));
        booking.setCheckOutDate(LocalDate.now().plusDays(33));
        booking.setAdultsCount(2);
        booking.setChildrenCount(1);
        booking.setRooms(rooms);
        booking.setTotalAmount(199.99 * 3 + 299.99 * 3); // 3 nights
        booking.setGuestName("John Smith");
        booking.setGuestEmail("john.smith@example.com");
        booking.setPaymentMethod("Credit Card");
        booking.setTransactionId("TXN" + System.currentTimeMillis());
        booking.setTaxAmount(booking.getTotalAmount() * 0.1); // 10% tax

        return booking;
    }
}