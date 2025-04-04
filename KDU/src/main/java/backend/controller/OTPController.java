package backend.controller;


import backend.service.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
@Slf4j
public class OTPController {

    private final OTPService otpService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        log.info("Generating OTP for email: {}", email);
        otpService.generateAndSendOTP(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent successfully to " + email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (email == null || email.isEmpty() || otp == null || otp.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
        }

        log.info("Verifying OTP for email: {}", email);
        boolean isValid = otpService.verifyOTP(email, otp);

        Map<String, Object> response = new HashMap<>();
        response.put("verified", isValid);
        response.put("message", isValid ? "OTP verification successful" : "OTP verification failed");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    public ResponseEntity<Map<String, String>> resendOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        log.info("Resending OTP for email: {}", email);
        otpService.resendOTP(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP resent successfully to " + email);
        return ResponseEntity.ok(response);
    }
}