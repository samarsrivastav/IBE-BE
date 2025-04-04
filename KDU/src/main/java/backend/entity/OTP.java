package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Data
@NoArgsConstructor
public class OTP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String otpValue;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean verified;
    private int attempts;

    public OTP(String email, String otpValue, int expiryMinutes) {
        this.email = email;
        this.otpValue = otpValue;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusMinutes(expiryMinutes);
        this.verified = false;
        this.attempts = 0;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}