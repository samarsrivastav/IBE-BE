package backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class RoomTypeResponseDTO {
    // Fields from GraphQL
    @JsonProperty("room_type_id")
    private Long id;
    
    @JsonProperty("room_type_name")
    private String name;
    
    @JsonProperty("max_capacity")
    private Integer maxOccupancy;
    
    @JsonProperty("area_in_square_feet")
    private Integer size;
    
    @JsonProperty("single_bed")
    private Integer singleBed;
    
    @JsonProperty("double_bed")
    private Integer doubleBed;

    // Fields from Local Database
    private String description;
    private Integer reviews;
    private Double rating;
    private JsonNode images;
    private JsonNode amenities;
    private String location;

    // Additional fields for response
    private Double price;  // Average rate for the date range
    private Integer availableRooms;  // Number of available rooms
} 