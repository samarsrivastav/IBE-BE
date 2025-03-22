package backend.service;

import backend.entity.PropertyConfiguration;
import backend.repository.PropertyConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PropertyConfigurationService {

    private final PropertyConfigurationRepository propertyConfigurationRepository;


    public PropertyConfiguration getConfiguration(Long propertyId) {
        return propertyConfigurationRepository.findByPropertyId(propertyId).orElse(null);
    }

    public PropertyConfiguration updateConfiguration(PropertyConfiguration config) {
        return propertyConfigurationRepository.save(config);
    }
}
