package backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BookingTransactionDTO {
    @JsonProperty("booking_id")
    private Long bookingId;
    
    @JsonProperty("booking_details")
    private BookingDetailsDTO bookingDetails;
}

@Data
class BookingDetailsDTO {
    @JsonProperty("confirmationDetails")
    private ConfirmationDetailsDTO confirmationDetails;
}

@Data
class ConfirmationDetailsDTO {
    @JsonProperty("startDate")
    private String startDate;
    
    @JsonProperty("endDate")
    private String endDate;
} 