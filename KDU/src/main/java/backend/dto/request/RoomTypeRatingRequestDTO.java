package backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeRatingRequestDTO {

    @NotNull(message = "Room type ID is required")
    private Long roomTypeId;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    @NotNull(message = "Rating is required")
    private Double rating;

    private String reviewText;
    private String token;      // For email review validation
    private String email;      // For email review validation
} 