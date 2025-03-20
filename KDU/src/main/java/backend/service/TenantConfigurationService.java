package backend.service;

import backend.entity.TenantConfiguration;
import backend.repository.TenantConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantConfigurationService {

    private final TenantConfigurationRepository repository;

    public List<TenantConfiguration> getAllConfigurations() {
        return repository.findAll();
    }

    public Optional<TenantConfiguration> getConfigurationById(String id) {
        return repository.findById(id);
    }

    public TenantConfiguration saveConfiguration(JsonNode configJson) {
        TenantConfiguration config = new TenantConfiguration();
        config.setConfigurationJson(configJson);
        return repository.save(config);
    }

    public void deleteConfiguration(String id) {
        repository.deleteById(id);
    }
}
