package backend.dto.request;

import backend.entity.enums.PromoType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PromoCodeValidationRequestDTO {
    @NotNull(message = "Usage date is required")
    private LocalDate usageDate;
    private PromoType promoType;
} 