package backend.controller;

import backend.dto.request.PurchaseRequestDto;
import backend.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/send-confirmation")
    public ResponseEntity<?> sendBookingConfirmation(@RequestBody PurchaseRequestDto request) {
        log.info("=== Starting Booking Confirmation Process ===");
        log.info("Received confirmation request for booking: {}, email: {}", 
                request.getConfirmationId(), request.getEmail());
        
        try {
            purchaseService.sendBookingConfirmation(request.getConfirmationId(), request.getEmail());
            return ResponseEntity.ok().body(Map.of(
                "status", "success",
                "message", "Booking confirmation email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending booking confirmation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to send booking confirmation: " + e.getMessage()
            ));
        }
    }
}