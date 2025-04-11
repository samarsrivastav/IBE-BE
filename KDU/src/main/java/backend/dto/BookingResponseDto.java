package backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private UUID confirmationId;
    private boolean isActive;
    private JsonNode bookingDetails;
} 