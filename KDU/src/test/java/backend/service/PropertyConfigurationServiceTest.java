package backend.service;

import backend.entity.PropertyConfiguration;
import backend.repository.PropertyConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropertyConfigurationServiceTest {

    @Mock
    private PropertyConfigurationRepository propertyConfigurationRepository;

    @InjectMocks
    private PropertyConfigurationService propertyConfigurationService;

    private PropertyConfiguration mockConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();

        mockConfig = new PropertyConfiguration();
        mockConfig.setId(1L);
        mockConfig.setPropertyId(101);
        mockConfig.setShowGuest(true);
        mockConfig.setWheelChairOption(false);
        mockConfig.setShowRoomNumber(true);
        mockConfig.setMaxGuestPerRoom(4);
        mockConfig.setMaxRoomsPerBooking(15);

        ObjectNode guestTypes = objectMapper.createObjectNode();
        guestTypes.put("adult", 18);
        guestTypes.put("child", 17);
        mockConfig.setGuestTypes(guestTypes);
    }

    @Test
    void getConfiguration_WhenPropertyExists_ShouldReturnConfig() {
        when(propertyConfigurationRepository.findByPropertyId(Long.valueOf(mockConfig.getPropertyId())))
                .thenReturn(Optional.of(mockConfig));

        PropertyConfiguration result = propertyConfigurationService.getConfiguration(Long.valueOf(mockConfig.getPropertyId()));

        assertNotNull(result);
        assertEquals(101, result.getPropertyId());
        assertTrue(result.isShowGuest());
        assertFalse(result.isWheelChairOption());
        assertTrue(result.isShowRoomNumber());
        assertEquals(4, result.getMaxGuestPerRoom());
        assertEquals(15, result.getMaxRoomsPerBooking());

        assertNotNull(result.getGuestTypes());
        assertEquals(18, result.getGuestTypes().get("adult").asInt());
        assertEquals(17, result.getGuestTypes().get("child").asInt());

        verify(propertyConfigurationRepository, times(1)).findByPropertyId(Long.valueOf(mockConfig.getPropertyId()));
    }

    @Test
    void getConfiguration_WhenPropertyDoesNotExist_ShouldReturnNull() {
        when(propertyConfigurationRepository.findByPropertyId(999L)).thenReturn(Optional.empty());

        PropertyConfiguration result = propertyConfigurationService.getConfiguration(999L);

        assertNull(result);
        verify(propertyConfigurationRepository, times(1)).findByPropertyId(999L);
    }

    @Test
    void updateConfiguration_WhenValidConfig_ShouldReturnUpdatedConfig() {
        when(propertyConfigurationRepository.save(mockConfig)).thenReturn(mockConfig);

        PropertyConfiguration result = propertyConfigurationService.updateConfiguration(mockConfig);

        assertNotNull(result);
        assertEquals(101, result.getPropertyId());
        assertTrue(result.isShowGuest());
        assertFalse(result.isWheelChairOption());
        assertTrue(result.isShowRoomNumber());
        assertEquals(4, result.getMaxGuestPerRoom());
        assertEquals(15, result.getMaxRoomsPerBooking());

        assertNotNull(result.getGuestTypes());
        assertEquals(18, result.getGuestTypes().get("adult").asInt());
        assertEquals(17, result.getGuestTypes().get("child").asInt());

        verify(propertyConfigurationRepository, times(1)).save(mockConfig);
    }
}
