package backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RoomAvailabilityResponseDTO {
    @JsonProperty("availability_id")
    private Long availabilityId;
    
    @JsonProperty("room_id")
    private Long roomId;
    
    @JsonProperty("booking_id")
    private Long bookingId;
    
    @JsonProperty("date")
    private String date;
} 