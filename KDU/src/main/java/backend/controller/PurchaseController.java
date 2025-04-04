package backend.controller;


import backend.entity.Booking;
import backend.service.OTPService;
import backend.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
@Slf4j
public class PurchaseController {

    private final OTPService otpService;
    private final PurchaseService purchaseService;

    // Map to temporarily store purchase IDs associated with emails
    private final Map<String, String> emailToPurchaseIdMap = new HashMap<>();

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePurchase(@RequestBody PurchaseRequest request) {
        // Validate request
        log.info("Initiating purchase process for user: {}", request.getEmail());

        // Initiate purchase
        String purchaseId = purchaseService.initiatePurchase(request.getEmail());

        // Store the mapping for later use
        emailToPurchaseIdMap.put(request.getEmail(), purchaseId);

        // Generate and send OTP
        otpService.generateAndSendOTP(request.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Please check your email for verification code");
        response.put("requiresOtp", true);
        response.put("purchaseId", purchaseId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPurchase(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");
        String purchaseId = request.get("purchaseId");

        // If purchaseId not provided, try to get from map
        if (purchaseId == null || purchaseId.isEmpty()) {
            purchaseId = emailToPurchaseIdMap.get(email);
            if (purchaseId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("verified", false);
                errorResponse.put("message", "Purchase session expired or not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }

        log.info("Verifying purchase with OTP for email: {}", email);

        // Verify OTP
        boolean isValid = otpService.verifyOTP(email, otp);

        if (isValid) {
            // Complete purchase
            Booking completedBooking = purchaseService.completePurchase(purchaseId);

            Map<String, Object> response = new HashMap<>();
            response.put("verified", true);
            response.put("message", "Purchase verified successfully. Confirmation email sent.");
            response.put("bookingReference", completedBooking.getBookingReference());

            // Clean up the mapping
            emailToPurchaseIdMap.remove(email);

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("verified", false);
            response.put("message", "Invalid verification code");

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        log.info("Resending OTP for email: {}", email);
        otpService.resendOTP(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Verification code resent successfully");

        return ResponseEntity.ok(response);
    }

    // Add a simple data class for the purchase request
    public static class PurchaseRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}