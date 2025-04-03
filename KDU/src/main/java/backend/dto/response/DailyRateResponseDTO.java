package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRateResponseDTO {
    private LocalDate date;
    private DayOfWeek day;
    private BigDecimal originalRate;

    private BigDecimal discountAmount;

} 