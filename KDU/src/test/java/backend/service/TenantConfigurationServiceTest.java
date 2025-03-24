package backend.service;

import backend.entity.TenantConfiguration;
import backend.repository.TenantConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantConfigurationServiceTest {

    @Mock
    private TenantConfigurationRepository repository;

    @InjectMocks
    private TenantConfigurationService tenantConfigurationService;

    private ObjectMapper objectMapper;
    private TenantConfiguration mockConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockConfig = new TenantConfiguration();
        mockConfig.setConfigId(1L);
        mockConfig.setTenantId(100L);
        mockConfig.setConfigurationJson(objectMapper.createObjectNode().put("key", "value"));
    }

    @Test
    void getConfigurationsByTenant_ShouldReturnList() {
        when(repository.findByTenantId(100L)).thenReturn(Arrays.asList(mockConfig));

        List<TenantConfiguration> result = tenantConfigurationService.getConfigurationsByTenant(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getTenantId());

        verify(repository, times(1)).findByTenantId(100L);
    }

    @Test
    void saveConfiguration_ShouldReturnSavedEntity() {
        JsonNode jsonConfig = objectMapper.createObjectNode().put("feature", "enabled");
        when(repository.save(any(TenantConfiguration.class))).thenReturn(mockConfig);

        TenantConfiguration savedConfig = tenantConfigurationService.saveConfiguration(100L, jsonConfig);

        assertNotNull(savedConfig);
        assertEquals(100L, savedConfig.getTenantId());
        assertEquals("value", savedConfig.getConfigurationJson().get("key").asText());

        verify(repository, times(1)).save(any(TenantConfiguration.class));
    }

    @Test
    void deleteConfiguration_ShouldInvokeRepositoryDelete() {
        String configId = "1";

        // Ensure existsById returns true before deletion
        when(repository.existsById(configId)).thenReturn(true);
        doNothing().when(repository).deleteById(configId);

        // Call the method
        tenantConfigurationService.deleteConfiguration(configId);

        // Verify existsById was checked before delete
        verify(repository, times(1)).existsById(configId);
        verify(repository, times(1)).deleteById(configId);
    }

    @Test
    void deleteConfiguration_ShouldNotInvokeDeleteIfNotExists() {
        String configId = "1";

        // Simulate non-existing ID
        when(repository.existsById(configId)).thenReturn(false);

        // Call the method
        tenantConfigurationService.deleteConfiguration(configId);

        // Verify existsById was checked but deleteById was NOT called
        verify(repository, times(1)).existsById(configId);
        verify(repository, never()).deleteById(configId);
    }
}
