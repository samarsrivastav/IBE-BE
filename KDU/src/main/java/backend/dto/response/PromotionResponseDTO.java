package backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PromotionResponseDTO {
    @JsonProperty("promotion_id")
    private Long promotionId;
    
    @JsonProperty("promotion_title")
    private String promotionTitle;
    
    @JsonProperty("promotion_description")
    private String promotionDescription;
    
    @JsonProperty("price_factor")
    private Double priceFactor;
    
    @JsonProperty("minimum_days_of_stay")
    private Integer minimumDaysOfStay;
    
    @JsonProperty("is_deactivated")
    private Boolean isDeactivated;
} 