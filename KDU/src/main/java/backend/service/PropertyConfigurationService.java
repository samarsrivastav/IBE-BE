package backend.service;

import backend.entity.PropertyConfiguration;
import backend.repository.PropertyConfigurationRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PropertyConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(PropertyConfigurationService.class);
    private final PropertyConfigurationRepository propertyConfigurationRepository;

    @Cacheable(value = "propertyCache", key = "#propertyId")
    public PropertyConfiguration getConfiguration(Long propertyId) {
        logger.info("Cache miss for propertyId: {} - Fetching from database", propertyId);

        try {
            return propertyConfigurationRepository.findByPropertyId(propertyId)
                    .orElseGet(() -> {
                        logger.warn("No configuration found for propertyId: {}", propertyId);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Error fetching configuration for propertyId: {} - {}", propertyId, e.getMessage(), e);
            return null;
        }
    }


    public PropertyConfiguration updateConfiguration(PropertyConfiguration config) {
        logger.info("Updating configuration for propertyId: {}", config.getPropertyId());

        try {
            PropertyConfiguration updatedConfig = propertyConfigurationRepository.save(config);
            logger.info("Successfully updated configuration for propertyId: {}", config.getPropertyId());
            return updatedConfig;
        } catch (Exception e) {
            logger.error("Error updating configuration for propertyId: {} - {}", config.getPropertyId(), e.getMessage(), e);
            return null;
        }
    }

}