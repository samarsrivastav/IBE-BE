package backend.service.impl;

import backend.entity.RoomTypes;
import backend.repository.RoomTypeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceImplTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomTypeServiceImpl roomTypeService;

    private List<RoomTypes> roomTypesList;

    @BeforeEach
    void setUp() {
        roomTypesList = Arrays.asList(
                new RoomTypes(1L, "Single Room", "Description for single room", 100, 4.5, null, null, "New York"),
                new RoomTypes(2L, "Double Room", "Description for double room", 200, 4.8, null, null, "Los Angeles")
        );
    }

    @Test
    void getAllRoomTypes_ShouldReturnAllRoomTypes() {
        when(roomTypeRepository.findAll()).thenReturn(roomTypesList);

        List<RoomTypes> result = roomTypeService.getAllRoomTypes();

        assertEquals(2, result.size());
        assertEquals("Single Room", result.get(0).getRoomTypeName());
        assertEquals("Double Room", result.get(1).getRoomTypeName());
    }
}