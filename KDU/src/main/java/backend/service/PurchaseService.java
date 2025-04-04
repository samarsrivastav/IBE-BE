package backend.service;

import backend.entity.Booking;
import backend.entity.Room;
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

    private final EmailService emailService;
    private final PDFService pdfService;

    // In-memory storage for temporary testing purposes
    private final Map<String, PurchaseInfo> pendingPurchases = new ConcurrentHashMap<>();

    /**
     * Initiates a purchase process
     * @param email User's email
     * @return Generated purchase ID
     */
    public String initiatePurchase(String email) {
        log.info("Initiating purchase process for email: {}", email);

        // Generate a purchase ID
        String purchaseId = UUID.randomUUID().toString();

        // Create temporary purchase info
        PurchaseInfo purchaseInfo = new PurchaseInfo();
        purchaseInfo.setEmail(email);
        purchaseInfo.setCreatedAt(LocalDateTime.now());
        purchaseInfo.setStatus("PENDING");

        // Store in the temporary map
        pendingPurchases.put(purchaseId, purchaseInfo);

        return purchaseId;
    }

    /**
     * Completes a purchase after OTP verification
     * @param purchaseId Purchase ID
     * @return Completed booking details
     */
    public Booking completePurchase(String purchaseId) {
        log.info("Completing purchase for ID: {}", purchaseId);

        // Get purchase info
        PurchaseInfo purchaseInfo = pendingPurchases.get(purchaseId);
        if (purchaseInfo == null) {
            log.error("Purchase ID not found: {}", purchaseId);
            throw new RuntimeException("Purchase not found");
        }

        // Update status
        purchaseInfo.setStatus("COMPLETED");
        purchaseInfo.setCompletedAt(LocalDateTime.now());

        // Create a sample booking (for testing purposes)
        Booking booking = createSampleBooking();
        booking.setGuestEmail(purchaseInfo.getEmail());

        // Send confirmation email
        try {
            emailService.sendBookingConfirmation(purchaseInfo.getEmail());
            log.info("Booking confirmation email sent to: {}", purchaseInfo.getEmail());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation", e);
            // Continue with process despite email error
        }

        return booking;
    }

    /**
     * Gets purchase info
     * @param purchaseId Purchase ID
     * @return Purchase information
     */
    public PurchaseInfo getPurchaseInfo(String purchaseId) {
        return pendingPurchases.get(purchaseId);
    }

    /**
     * Cancels a pending purchase
     * @param purchaseId Purchase ID
     * @return True if cancelled successfully
     */
    public boolean cancelPurchase(String purchaseId) {
        PurchaseInfo purchaseInfo = pendingPurchases.get(purchaseId);
        if (purchaseInfo != null && "PENDING".equals(purchaseInfo.getStatus())) {
            purchaseInfo.setStatus("CANCELLED");
            log.info("Purchase cancelled: {}", purchaseId);
            return true;
        }
        return false;
    }

    /**
     * Helper method to create sample booking data
     */
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
        booking.setPaymentMethod("Credit Card");
        booking.setTransactionId("TXN" + System.currentTimeMillis());
        booking.setTaxAmount(booking.getTotalAmount() * 0.1); // 10% tax

        return booking;
    }

    /**
     * Internal class to store purchase information temporarily
     */
    private static class PurchaseInfo {
        private String email;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }
    }
}