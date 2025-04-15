package backend.service;

import backend.dto.request.RoomTypeSearchRequestDTO;
import backend.dto.response.RoomTypeResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeAvailabilityServiceTest {

    @Mock
    private RoomTypeAvailabilityService roomTypeAvailabilityService;

    @BeforeEach
    void setUp() {
        roomTypeAvailabilityService = mock(RoomTypeAvailabilityService.class);
    }

    @Test
    void testGetAvailableRoomTypes() {
        // Arrange
        RoomTypeSearchRequestDTO searchRequest = new RoomTypeSearchRequestDTO();
        RoomTypeResponseDTO room1 = new RoomTypeResponseDTO();
        RoomTypeResponseDTO room2 = new RoomTypeResponseDTO();
        List<RoomTypeResponseDTO> mockResponse = Arrays.asList(room1, room2);

        when(roomTypeAvailabilityService.getAvailableRoomTypes(searchRequest)).thenReturn(mockResponse);

        // Act
        List<RoomTypeResponseDTO> result = roomTypeAvailabilityService.getAvailableRoomTypes(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(roomTypeAvailabilityService, times(1)).getAvailableRoomTypes(searchRequest);
    }

    @Test
    void testGetRoomTypeRate() {
        // Arrange
        Long roomTypeId = 1L;
        double mockRate = 150.0;

        when(roomTypeAvailabilityService.getRoomTypeRate(roomTypeId)).thenReturn(mockRate);

        // Act
        double result = roomTypeAvailabilityService.getRoomTypeRate(roomTypeId);

        // Assert
        assertEquals(mockRate, result);
        verify(roomTypeAvailabilityService, times(1)).getRoomTypeRate(roomTypeId);
    }

    @Test
    void testGetAverageRates() {
        // Arrange
        Map<Long, Double> mockRates = Map.of(1L, 150.0, 2L, 200.0);

        when(roomTypeAvailabilityService.getAverageRates()).thenReturn(mockRates);

        // Act
        Map<Long, Double> result = roomTypeAvailabilityService.getAverageRates();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(roomTypeAvailabilityService, times(1)).getAverageRates();
    }
}

