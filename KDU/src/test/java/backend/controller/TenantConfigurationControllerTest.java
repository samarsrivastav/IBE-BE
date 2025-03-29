package backend.controller;

import backend.entity.TenantConfiguration;
import backend.exception.TenantNotAllowedException;
import backend.service.TenantConfigurationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Ensure Mockito initializes mocks correctly
class TenantConfigurationControllerTest {

    @Mock
    private TenantConfigurationService service;

    @InjectMocks
    private TenantConfigurationController controller;

    private TenantConfiguration tenantConfig;
    private JsonNode configJson;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tenantConfig = new TenantConfiguration();
        tenantConfig.setConfigId(1L);
        tenantConfig.setTenantId(1L);
        configJson = objectMapper.createObjectNode().put("key", "value");
        tenantConfig.setConfigurationJson(configJson);
    }

    @Test
    void getConfigurationsByTenant_ValidTenantId_ShouldReturnList() {
        when(service.getConfigurationsByTenant(1L)).thenReturn(Collections.singletonList(tenantConfig));

        List<TenantConfiguration> response = controller.getConfigurationsByTenant(1L);

        System.out.println("Service returned: " + response); // Debugging output

        assertNotNull(response, "response should not be null");
        assertEquals(1, response.size(), "response list size should be 1");
        assertEquals(tenantConfig, response.get(0), "response should match the mock object");

        verify(service, times(1)).getConfigurationsByTenant(1L);
    }

    @Test
    void getConfigurationsByTenant_InvalidTenantId_ShouldThrowException() {
        Exception exception = assertThrows(TenantNotAllowedException.class, () -> controller.getConfigurationsByTenant(2L));
        assertEquals("Access denied for tenant: 2", exception.getMessage());
    }

    @Test
    void createConfiguration_ValidTenant_ShouldReturnSavedConfig() {
        when(service.saveConfiguration(1L, configJson)).thenReturn(tenantConfig);

        TenantConfiguration response = controller.createConfiguration(1L, configJson);

        System.out.println("Saved Configuration: " + response); // Debugging output

        assertNotNull(response, "response should not be null");
        assertEquals(tenantConfig, response, "response should match the mock object");

        verify(service, times(1)).saveConfiguration(1L, configJson);
    }

    @Test
    void createConfiguration_InvalidTenant_ShouldThrowException() {
        Exception exception = assertThrows(TenantNotAllowedException.class, () -> controller.createConfiguration(2L, configJson));
        assertEquals("Access denied for tenant: 2", exception.getMessage());
    }

    @Test
    void deleteConfiguration_ValidTenant_ShouldDeleteConfig() {
        doNothing().when(service).deleteConfiguration("1");

        ResponseEntity<Void> response = controller.deleteConfiguration(1L, "1");

        assertEquals(204, response.getStatusCodeValue(), "response status should be 204 No Content");

        verify(service, times(1)).deleteConfiguration("1");
    }

    @Test
    void deleteConfiguration_InvalidTenant_ShouldThrowException() {
        Exception exception = assertThrows(TenantNotAllowedException.class, () -> controller.deleteConfiguration(2L, "1"));
        assertEquals("Access denied for tenant: 2", exception.getMessage());
    }
}
