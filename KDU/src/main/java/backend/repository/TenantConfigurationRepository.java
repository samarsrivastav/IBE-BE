package backend.repository;

import backend.entity.TenantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, String> {
}
