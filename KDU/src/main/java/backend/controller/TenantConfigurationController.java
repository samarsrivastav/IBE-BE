package backend.controller;

import backend.entity.TenantConfiguration;
import backend.exception.TenantNotAllowedException;
import backend.service.TenantConfigurationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/config/tenant")
@RequiredArgsConstructor
public class TenantConfigurationController {

    private final TenantConfigurationService service;

    @GetMapping("/{tenantId}")
    public List<TenantConfiguration> getConfigurationsByTenant(@PathVariable Long tenantId) {
        validateTenant(tenantId);
        return service.getConfigurationsByTenant(tenantId);
    }

    @PostMapping("/{tenantId}")
    public TenantConfiguration createConfiguration(@PathVariable Long tenantId, @RequestBody JsonNode configJson) {
        validateTenant(tenantId);
        return service.saveConfiguration(tenantId, configJson);
    }

    @DeleteMapping("/{tenantId}/{configId}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long tenantId, @PathVariable String configId) {
        validateTenant(tenantId);
        service.deleteConfiguration(configId);
        return ResponseEntity.noContent().build();
    }

    private void validateTenant(Long tenantId) {
        if (!tenantId.equals(1L)) {
            throw new TenantNotAllowedException("Access denied for tenant: " + tenantId);
        }
    }
}
