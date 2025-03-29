package backend.dto.response;

import backend.entity.Enum.PromoType;
import lombok.Data;

@Data
public class PromoCodeResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String title;
    private Double discount;
    private PromoType promoType;
} 