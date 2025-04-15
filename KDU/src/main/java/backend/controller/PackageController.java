package backend.controller;

import backend.dto.request.PackageSearchRequestDTO;
import backend.dto.response.RoomTypePackagesDTO;
import backend.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
@Tag(name = "Package Controller", description = "APIs for managing room packages and promotions")
public class PackageController {

    private final PackageService packageService;

    @PostMapping
    @Operation(summary = "Get all available packages for room types", 
              description = "Retrieves all available packages and promotions for room types based on search criteria")
    public ResponseEntity<List<RoomTypePackagesDTO>> getAllPackages(@Valid @RequestBody PackageSearchRequestDTO searchRequest) {
        try {
            log.info("Getting packages for property {} between {} and {}", 
                searchRequest.getPropertyId(), searchRequest.getStartDate(), searchRequest.getEndDate());
            
            List<RoomTypePackagesDTO> packages = packageService.getAllPackages(searchRequest);
            
            if (packages.isEmpty()) {
                log.info("No packages found for the given criteria");
                return ResponseEntity.ok(packages);
            }
            
            log.info("Found {} room types with packages", packages.size());
            return ResponseEntity.ok(packages);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to get packages: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
} 