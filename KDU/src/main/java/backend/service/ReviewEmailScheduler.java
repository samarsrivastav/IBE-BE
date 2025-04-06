package backend.service;

import backend.model.BookingTransaction;
import backend.repository.BookingTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewEmailScheduler {

    private final BookingTransactionRepository bookingTransactionRepository;
    private final ReviewEmailService reviewEmailService;

    @Scheduled(cron = "0 * * * * ?") // Run every minute for testing
    public void sendReviewEmails() {
        System.out.println("=== Review Email Scheduler Started ===");
        
        // For testing, we'll check bookings from today
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ISO_DATE);
        
        System.out.println("Checking for bookings with checkout date: " + formattedDate);
        
        try {
            List<BookingTransaction> completedBookings = bookingTransactionRepository
                    .findByCheckOutDate(formattedDate);
            
            System.out.println("Found " + completedBookings.size() + " bookings to process");
            
            if (completedBookings.isEmpty()) {
                System.out.println("No bookings found for today's checkout date");
                return;
            }

            for (BookingTransaction booking : completedBookings) {
                try {
                    System.out.println("Processing booking ID: " + booking.getId());
                    System.out.println("Booking email: " + booking.getEmail());
                    System.out.println("Booking details: " + booking.getBookingDetails());
                    
                    reviewEmailService.sendReviewRequestEmail(booking);
                    System.out.println("Review email sent successfully for booking: " + booking.getId());
                } catch (Exception e) {
                    System.err.println("Failed to send review email for booking: " + booking.getId());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in scheduler: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== Review Email Scheduler Completed ===");
    }
} 