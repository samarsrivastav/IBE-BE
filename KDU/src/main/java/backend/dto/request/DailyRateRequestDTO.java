package backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRateRequestDTO {
    private Long roomTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String promotionTitle;  // Can be standard promotion, custom promotion, or promo code title
} 