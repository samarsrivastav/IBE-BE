package backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RoomTypeSearchRequestDTO {
    
    @NotNull(message = "Property ID is required")
    private Integer propertyId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "Number of guests must be at least 1")
    private Integer numberOfGuests;

    @Min(value = 1, message = "Number of rooms must be at least 1")
    private Integer numberOfRooms;

    @Min(value = 1, message = "Total beds must be at least 1")
    private Integer totalBeds;

    @Min(value = 1, message = "Page size must be at least 1")
    private Integer pageSize;

    private String cursor;
} 