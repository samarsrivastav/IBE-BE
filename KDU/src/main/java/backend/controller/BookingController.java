package backend.controller;

import backend.dto.OTPDto;
import backend.dto.request.BookingRequestDto;
import backend.dto.request.BookingVerificationRequestDto;
import backend.model.BookingTransaction;
import backend.repository.BookingTransactionRepository;
import backend.service.BookingService;
import backend.service.OTPService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final OTPService otpService;
    private final BookingTransactionRepository bookingTransactionRepository;

    @PostMapping("/initiate")
    public ResponseEntity<OTPDto.OTPResponse> initiateBooking(@RequestBody BookingRequestDto bookingRequestDto) {
        log.info("=== Starting Booking Initiation Process ===");
        log.info("Received booking request for property: {}, room type: {}", 
                bookingRequestDto.getConfirmationDetails().getPropertyId(),
                bookingRequestDto.getConfirmationDetails().getRoomTypeId());
        log.info("Guest email: {}", bookingRequestDto.getBillingInfo().getEmail());
        log.info("Booking dates: {} to {}", 
                bookingRequestDto.getConfirmationDetails().getStartDate(),
                bookingRequestDto.getConfirmationDetails().getEndDate());
        
        try {
            // Generate and send OTP
            log.info("Generating and sending OTP to: {}", bookingRequestDto.getBillingInfo().getEmail());
            otpService.generateAndSendOTP(bookingRequestDto.getBillingInfo().getEmail());
            log.info("OTP sent successfully");
            
            return ResponseEntity.ok(new OTPDto.OTPResponse("OTP sent successfully"));
        } catch (Exception e) {
            log.error("Error during booking initiation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new OTPDto.OTPResponse("Failed to initiate booking: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<OTPDto.OTPVerificationResponse> verifyOTPAndProcessBooking(
            @RequestBody BookingVerificationRequestDto verificationRequest) {
        
        log.info("=== Starting OTP Verification and Booking Process ===");
        log.info("Verifying OTP for email: {}", verificationRequest.getOtpRequest().getEmail());
        log.info("Booking details - Property: {}, Room Type: {}", 
                verificationRequest.getBookingRequestDto().getConfirmationDetails().getPropertyId(),
                verificationRequest.getBookingRequestDto().getConfirmationDetails().getRoomTypeId());
        
        try {
            // Verify OTP
            log.info("Attempting OTP verification");
            boolean isValid = otpService.verifyOTP(
                    verificationRequest.getOtpRequest().getEmail(), 
                    verificationRequest.getOtpRequest().getOtp());
            
            if (!isValid) {
                log.warn("Invalid OTP provided for email: {}", verificationRequest.getOtpRequest().getEmail());
                return ResponseEntity.ok(new OTPDto.OTPVerificationResponse(false, "Invalid OTP"));
            }
            log.info("OTP verification successful");

            // Process booking after successful OTP verification
            log.info("Processing booking after successful OTP verification");
            UUID confirmationId = bookingService.checkIfBookingIsPossible(
                    verificationRequest.getBookingRequestDto().getConfirmationDetails().getPropertyId(),
                    verificationRequest.getBookingRequestDto().getConfirmationDetails().getStartDate(),
                    verificationRequest.getBookingRequestDto().getConfirmationDetails().getEndDate(),
                    verificationRequest.getBookingRequestDto().getConfirmationDetails().getRoomTypeId(),
                    verificationRequest.getBookingRequestDto().getConfirmationDetails().getRoomCount(),
                    verificationRequest.getBookingRequestDto()
            );
            
            log.info("Booking processed successfully. Confirmation ID: {}", confirmationId);
            return ResponseEntity.ok(new OTPDto.OTPVerificationResponse(true, 
                "Booking confirmed successfully. Confirmation ID: " + confirmationId));
        } catch (Exception e) {
            log.error("Error during OTP verification and booking process: {}", e.getMessage(), e);
            return ResponseEntity.ok(new OTPDto.OTPVerificationResponse(false, 
                "Failed to process booking: " + e.getMessage()));
        }
    }

    @GetMapping("/confirmation/{confirmationId}")
    public ResponseEntity<JsonNode> getBookingConfirmationStatus(@PathVariable UUID confirmationId) {
        log.info("=== Checking Booking Confirmation Status ===");
        log.info("Checking status for confirmation ID: {}", confirmationId);
        
        try {
            BookingTransaction bookingTransaction = bookingTransactionRepository.findByConfirmationId(confirmationId)
                    .orElseThrow(() -> new RuntimeException("Booking not found for confirmation ID: " + confirmationId));
            
            log.info("Found booking transaction with ID: {}", bookingTransaction.getId());
            log.info("Booking is active: {}", bookingTransaction.isActive());
            
            // Return the complete booking details as stored in the database
            JsonNode bookingDetails = bookingTransaction.getBookingDetails();
            log.info("Returning complete booking details for confirmation ID: {}", confirmationId);
            
            return ResponseEntity.ok(bookingDetails);
        } catch (Exception e) {
            log.error("Error checking booking confirmation status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
} 