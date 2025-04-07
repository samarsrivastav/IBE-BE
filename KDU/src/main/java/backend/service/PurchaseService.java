package backend.service;

import backend.entity.Booking;
import backend.entity.Room;

import backend.model.BookingTransaction;
import backend.repository.BookingTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    private final BookingTransactionRepository bookingTransactionRepository;
    private final EmailService emailService;
    private final PDFService pdfService;

    public void sendBookingConfirmation(UUID confirmationId, String email) {
        log.info("=== Starting Booking Confirmation Process ===");
        log.info("Sending confirmation for booking: {} to email: {}", confirmationId, email);

        try {
            // Fetch booking transaction
            BookingTransaction transaction = bookingTransactionRepository.findByConfirmationId(confirmationId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Verify email matches
            if (!email.equals(transaction.getEmail())) {
                throw new RuntimeException("Email does not match booking record");
            }

            // Send confirmation email
            emailService.sendBookingConfirmation(email, transaction);
            log.info("Booking confirmation email sent successfully");

        } catch (Exception e) {
            log.error("Error sending booking confirmation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send booking confirmation: " + e.getMessage());
        }
    }
}