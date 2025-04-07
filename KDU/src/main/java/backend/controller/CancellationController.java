package backend.controller;

import backend.dto.OTPDto;
import backend.dto.request.CancellationRequestDto;
import backend.service.CancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cancellations")
public class CancellationController {

    private final CancellationService cancellationService;

    @PostMapping("/initiate")
    public ResponseEntity<OTPDto.OTPResponse> initiateCancellation(
            @RequestBody CancellationRequestDto request) {
        log.info("=== Starting Cancellation Initiation ===");
        log.info("Received cancellation request for booking: {}, email: {}", 
                request.getConfirmationId(), request.getEmail());
        
        try {
            OTPDto.OTPResponse response = cancellationService.initiateCancellation(
                    request.getEmail(), 
                    request.getConfirmationId()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during cancellation initiation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new OTPDto.OTPResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<OTPDto.OTPVerificationResponse> verifyAndCancel(
            @RequestBody CancellationRequestDto request) {
        log.info("=== Starting Cancellation Verification ===");
        log.info("Verifying cancellation for booking: {}, email: {}", 
                request.getConfirmationId(), request.getEmail());
        
        try {
            OTPDto.OTPVerificationResponse response = cancellationService.verifyOTPAndCancelBooking(
                    request.getEmail(),
                    request.getOtp(),
                    request.getConfirmationId()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during cancellation verification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new OTPDto.OTPVerificationResponse(false, e.getMessage()));
        }
    }
} 