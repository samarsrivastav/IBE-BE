package backend.repository;

import backend.entity.PropertyConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyConfigurationRepository extends JpaRepository<PropertyConfiguration, Long> {
    Optional<PropertyConfiguration> findByPropertyId(Long propertyId);
}
