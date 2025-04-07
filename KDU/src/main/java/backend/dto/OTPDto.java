package backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class OTPDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OTPRequest {
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OTPVerificationRequest {
        private String email;
        private String otp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OTPResponse {
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OTPVerificationResponse {
        private boolean verified;
        private String message;
    }
}