package backend.service;

import backend.model.BookingTransaction;
import backend.repository.BookingTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEmailScheduler {

    private final BookingTransactionRepository bookingTransactionRepository;
    private final ReviewEmailService reviewEmailService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Scheduled(cron = "0 12 12 * * *") //
    public void sendPendingReviewEmails() {
        log.info("Starting daily review email sending task");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        
        List<BookingTransaction> transactions = bookingTransactionRepository.findAll();
        int emailsSent = 0;
        
        for (BookingTransaction transaction : transactions) {
            try {
                if (isCheckoutInLastDay(transaction, twentyFourHoursAgo, now)) {
                    log.info("Sending review email for booking: {}", transaction.getId());
                    reviewEmailService.sendReviewRequestEmail(transaction);
                    emailsSent++;
                    log.info("Review email sent successfully for booking: {}", transaction.getId());
                }
            } catch (Exception e) {
                log.error("Error processing review email for booking {}: {}", transaction.getId(), e.getMessage());
            }
        }
        
        log.info("Completed daily review email sending task. Sent {} emails", emailsSent);
    }
    
    private boolean isCheckoutInLastDay(BookingTransaction transaction, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            JsonNode bookingDetails = transaction.getBookingDetails();
            if (bookingDetails != null && bookingDetails.has("confirmationDetails")) {
                JsonNode confirmationDetails = bookingDetails.get("confirmationDetails");
                if (confirmationDetails.has("endDate")) {
                    String endDateStr = confirmationDetails.get("endDate").asText();
                    LocalDate checkoutDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
                    
                    // Convert checkout date to the end of that day for comparison
                    LocalDateTime checkoutDateTime = checkoutDate.atTime(23, 59, 59);
                    
                    // Check if checkout happened in the last 24 hours
                    return checkoutDateTime.isAfter(startTime) && checkoutDateTime.isBefore(endTime);
                }
            }
        } catch (Exception e) {
            log.error("Error checking checkout date for booking {}: {}", 
                     transaction.getId(), e.getMessage());
        }
        return false;
    }
} 