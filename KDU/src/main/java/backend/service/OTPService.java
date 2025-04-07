package backend.service;

import backend.entity.OTP;
import backend.exception.OtpException;
import backend.repository.OTPRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final OTPRepository otpRepository;
    private final EmailService emailService;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    @Value("${otp.max.attempts}")
    private int maxAttempts;

    @Transactional
    public void generateAndSendOTP(String email) {
        String otp = generateOTP();
        OTP otpEntity = new OTP(email, otp, otpExpiryMinutes);
        otpRepository.save(otpEntity);
        emailService.sendOtpEmail(email, otp);
        log.info("OTP generated and sent to: {}", email);
    }

    @Transactional
    public boolean verifyOTP(String email, String otpValue) {
        OTP otp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new OtpException("No OTP found for this email"));

        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);

        // Check attempts
        if (otp.getAttempts() > maxAttempts) {
            log.warn("Max OTP verification attempts exceeded for email: {}", email);
            throw new OtpException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        // Check expiry
        if (otp.isExpired()) {
            log.warn("OTP expired for email: {}", email);
            throw new OtpException("OTP has expired. Please request a new OTP.");
        }

        // Check if already verified
        if (otp.isVerified()) {
            log.warn("OTP already verified for email: {}", email);
            throw new OtpException("OTP already verified");
        }

        // Verify OTP
        boolean isValid = otpValue.equals(otp.getOtpValue());
        if (isValid) {
            otp.setVerified(true);
            log.info("OTP verified successfully for email: {}", email);
        } else {
            log.warn("Invalid OTP provided for email: {}. Attempt: {}/{}", email, otp.getAttempts(), maxAttempts);
            if (otp.getAttempts() >= maxAttempts) {
                throw new OtpException("Maximum verification attempts reached. Please request a new OTP.");
            }
            throw new OtpException("Invalid OTP. Attempts left: " + (maxAttempts - otp.getAttempts()));
        }

        otpRepository.save(otp);
        return isValid;
    }

    
    private String generateOTP() {
        // Generate 6-digit OTP
        Random random = new Random();
        int otpValue = 100000 + random.nextInt(900000);
        return String.valueOf(otpValue);
    }
}