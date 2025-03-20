package backend.controller;
import backend.entity.TenantConfiguration;
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

    @GetMapping
    public List<TenantConfiguration> getAllConfigurations() {
        return service.getAllConfigurations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantConfiguration> getConfigurationById(@PathVariable String id) {
        Optional<TenantConfiguration> config = service.getConfigurationById(id);
        return config.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public TenantConfiguration createConfiguration(@RequestBody JsonNode configJson) {
        return service.saveConfiguration(configJson);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable String id) {
        service.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}
