package backend.service;

import backend.entity.TenantConfiguration;
import backend.repository.TenantConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(TenantConfigurationService.class);
    private final TenantConfigurationRepository repository;

    @Cacheable(value = "tenantConfigurations", key = "#tenantId")
    public List<TenantConfiguration> getConfigurationsByTenant(Long tenantId) {
        logger.info("Cache miss for tenantId: {} - Fetching configurations from database", tenantId);

        try {
            List<TenantConfiguration> configurations = repository.findByTenantId(tenantId);

            if (configurations.isEmpty()) {
                logger.warn("No configurations found for tenantId: {}", tenantId);
            } else {
                logger.info("Found {} configurations for tenantId: {}", configurations.size(), tenantId);
            }

            return configurations;
        } catch (Exception e) {
            logger.error("Error fetching configurations for tenantId: {} - {}", tenantId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public TenantConfiguration saveConfiguration(Long tenantId, JsonNode configJson) {
        logger.info("Saving configuration for tenantId: {}", tenantId);
        logger.debug("Configuration JSON: {}", configJson);

        try {
            TenantConfiguration config = new TenantConfiguration();
            config.setTenantId(tenantId);
            config.setConfigurationJson(configJson);

            TenantConfiguration savedConfig = repository.save(config);
            logger.info("Successfully saved configuration for tenantId: {}", tenantId);
            return savedConfig;
        } catch (Exception e) {
            logger.error("Error saving configuration for tenantId: {} - {}", tenantId, e.getMessage(), e);
            return null;
        }
    }

    public void deleteConfiguration(String configId) {
        logger.info("Deleting configuration with configId: {}", configId);

        try {
            if (repository.existsById(configId)) {
                repository.deleteById(configId);
                logger.info("Successfully deleted configuration with configId: {}", configId);
            } else {
                logger.warn("No configuration found with configId: {}", configId);
            }
        } catch (Exception e) {
            logger.error("Error deleting configuration with configId: {} - {}", configId, e.getMessage(), e);
        }
    }
}
