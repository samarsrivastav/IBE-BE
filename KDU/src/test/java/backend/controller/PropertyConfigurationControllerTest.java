package backend.controller;

import backend.entity.PropertyConfiguration;
import backend.service.PropertyConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyConfigurationControllerTest {

    @Mock
    private PropertyConfigurationService propertyConfigurationService;

    @InjectMocks
    private PropertyConfigurationController propertyConfigurationController;

    private PropertyConfiguration propertyConfiguration;

    @BeforeEach
    void setUp() {
        propertyConfiguration = new PropertyConfiguration();
        propertyConfiguration.setPropertyId(1); // Changed from Long to Integer
        propertyConfiguration.setMaxGuestPerRoom(4); // Setting a valid field
    }

    @Test
    void getConfig_ValidPropertyId_ShouldReturnConfiguration() {
        when(propertyConfigurationService.getConfiguration(1L)).thenReturn(propertyConfiguration);

        ResponseEntity<PropertyConfiguration> response = propertyConfigurationController.getConfig(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(propertyConfiguration, response.getBody());
        verify(propertyConfigurationService, times(1)).getConfiguration(1L);
    }

    @Test
    void updateConfig_ValidConfiguration_ShouldReturnUpdatedConfiguration() {
        when(propertyConfigurationService.updateConfiguration(any(PropertyConfiguration.class)))
                .thenReturn(propertyConfiguration);

        ResponseEntity<PropertyConfiguration> response = propertyConfigurationController.updateConfig(propertyConfiguration);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(propertyConfiguration, response.getBody());
        verify(propertyConfigurationService, times(1)).updateConfiguration(propertyConfiguration);
    }
}
