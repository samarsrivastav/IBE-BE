package backend.controller;
import backend.entity.PropertyConfiguration;
import backend.service.PropertyConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/config/property")
@RequiredArgsConstructor
public class PropertyConfigurationController {

    private final PropertyConfigurationService propertyConfigurationService;

    @GetMapping("/{propertyId}")
    public ResponseEntity<PropertyConfiguration> getConfig(@PathVariable Long propertyId) {
        return ResponseEntity.ok(propertyConfigurationService.getConfiguration(propertyId));
    }

    @PutMapping
    public ResponseEntity<PropertyConfiguration> updateConfig(@RequestBody PropertyConfiguration config) {
        return ResponseEntity.ok(propertyConfigurationService.updateConfiguration(config));
    }
}

