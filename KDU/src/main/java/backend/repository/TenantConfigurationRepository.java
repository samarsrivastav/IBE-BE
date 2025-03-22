package backend.repository;

import backend.entity.TenantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, String> {
    List<TenantConfiguration> findByTenantId(Long tenantId);
}
