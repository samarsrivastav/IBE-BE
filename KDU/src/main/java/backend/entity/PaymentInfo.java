package backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Invalid card number format")
    @Column(name = "card_number", nullable = false)
    private String cardNumber;

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Invalid expiry month")
    @Max(value = 12, message = "Invalid expiry month")
    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    @Min(value = 2024, message = "Invalid expiry year")
    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "billing_id", nullable = false)
    private Long billingId;
} 