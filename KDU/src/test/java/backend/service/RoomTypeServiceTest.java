package backend.service;

import backend.entity.RoomTypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTest {

    @Mock
    private RoomTypeService roomTypeService;

    @BeforeEach
    void setUp() {
        roomTypeService = mock(RoomTypeService.class);
    }

    @Test
    void testGetAllRoomTypes() {
        // Arrange
        RoomTypes room1 = new RoomTypes();
        RoomTypes room2 = new RoomTypes();
        List<RoomTypes> mockResponse = Arrays.asList(room1, room2);

        when(roomTypeService.getAllRoomTypes()).thenReturn(mockResponse);

        // Act
        List<RoomTypes> result = roomTypeService.getAllRoomTypes();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(roomTypeService, times(1)).getAllRoomTypes();
    }
}
