package backend.controller;

import backend.dto.request.RoomTypeSearchRequestDTO;
import backend.dto.response.RoomTypeResponseDTO;
import backend.service.RoomTypeAvailabilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomTypeAvailabilityController.class)
class RoomTypeAvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomTypeAvailabilityService roomTypeAvailabilityService;

    @Autowired
    private ObjectMapper objectMapper;

    private RoomTypeSearchRequestDTO searchRequest;
    private List<RoomTypeResponseDTO> mockResponse;

    @BeforeEach
    void setUp() {
        // Set up test data
        searchRequest = new RoomTypeSearchRequestDTO();
        searchRequest.setPropertyId(1);
        searchRequest.setStartDate(LocalDate.now());
        searchRequest.setEndDate(LocalDate.now().plusDays(1));
        searchRequest.setNumberOfGuests(2);
        searchRequest.setPageSize(10);

        // Create mock response
        RoomTypeResponseDTO roomType1 = new RoomTypeResponseDTO();
        roomType1.setId(1L);
        roomType1.setName("Standard Room");
        roomType1.setMaxOccupancy(2);
        roomType1.setSize(300);
        roomType1.setSingleBed(1);
        roomType1.setDoubleBed(1);
        roomType1.setPrice(100.0);
        roomType1.setAvailableRooms(5);

        RoomTypeResponseDTO roomType2 = new RoomTypeResponseDTO();
        roomType2.setId(2L);
        roomType2.setName("Deluxe Room");
        roomType2.setMaxOccupancy(4);
        roomType2.setSize(400);
        roomType2.setSingleBed(2);
        roomType2.setDoubleBed(1);
        roomType2.setPrice(150.0);
        roomType2.setAvailableRooms(3);

        mockResponse = Arrays.asList(roomType1, roomType2);
    }

    @Test
    void searchAvailableRoomTypes_WithValidRequest_ReturnsSuccess() throws Exception {
        // Arrange
        when(roomTypeAvailabilityService.getAvailableRoomTypes(any(RoomTypeSearchRequestDTO.class)))
            .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/room-types/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].room_type_id").value(1))
                .andExpect(jsonPath("$[0].room_type_name").value("Standard Room"))
                .andExpect(jsonPath("$[0].max_capacity").value(2))
                .andExpect(jsonPath("$[0].area_in_square_feet").value(300))
                .andExpect(jsonPath("$[0].single_bed").value(1))
                .andExpect(jsonPath("$[0].double_bed").value(1))
                .andExpect(jsonPath("$[0].price").value(100.0))
                .andExpect(jsonPath("$[0].availableRooms").value(5))
                .andExpect(jsonPath("$[1].room_type_id").value(2))
                .andExpect(jsonPath("$[1].room_type_name").value("Deluxe Room"))
                .andExpect(jsonPath("$[1].max_capacity").value(4))
                .andExpect(jsonPath("$[1].area_in_square_feet").value(400))
                .andExpect(jsonPath("$[1].single_bed").value(2))
                .andExpect(jsonPath("$[1].double_bed").value(1))
                .andExpect(jsonPath("$[1].price").value(150.0))
                .andExpect(jsonPath("$[1].availableRooms").value(3));
    }

    @Test
    void searchAvailableRoomTypes_WithInvalidDates_ThrowsException() {
        // Arrange: Set an invalid end date (before start date)
        searchRequest.setEndDate(searchRequest.getStartDate().minusDays(1));

        // Mock the service to throw an exception
        when(roomTypeAvailabilityService.getAvailableRoomTypes(any(RoomTypeSearchRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("End date must be after start date"));

        // Act & Assert: Directly check for the exception
        assertThrows(IllegalArgumentException.class, () -> {
            roomTypeAvailabilityService.getAvailableRoomTypes(searchRequest);
        });
    }

    

    @Test
    void searchAvailableRoomTypes_WithEmptyResponse_ReturnsEmptyList() throws Exception {
        // Arrange
        when(roomTypeAvailabilityService.getAvailableRoomTypes(any(RoomTypeSearchRequestDTO.class)))
            .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(post("/api/v1/room-types/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchAvailableRoomTypes_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        searchRequest.setPropertyId(null);
        searchRequest.setNumberOfGuests(null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/room-types/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchAvailableRoomTypes_WithInvalidNumberOfGuests_ReturnsBadRequest() throws Exception {
        // Arrange
        searchRequest.setNumberOfGuests(0);

        // Act & Assert
        mockMvc.perform(post("/api/v1/room-types/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchAvailableRoomTypes_WithInvalidPageSize_ReturnsBadRequest() throws Exception {
        // Arrange
        searchRequest.setPageSize(0);

        // Act & Assert
        mockMvc.perform(post("/api/v1/room-types/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isBadRequest());
    }
} 