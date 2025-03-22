package backend.service;

import backend.entity.TenantConfiguration;
import backend.repository.TenantConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantConfigurationService {

    private final TenantConfigurationRepository repository;

    public List<TenantConfiguration> getConfigurationsByTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public TenantConfiguration saveConfiguration(Long tenantId, JsonNode configJson) {
        TenantConfiguration config = new TenantConfiguration();
        config.setTenantId(tenantId);
        config.setConfigurationJson(configJson);
        return repository.save(config);
    }

    public void deleteConfiguration(String configId) {
        repository.deleteById(configId);
    }
}
