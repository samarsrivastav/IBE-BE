package backend.controller;

import backend.dto.response.PromotionResponseDTO;
import backend.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "APIs for managing promotions")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(
        summary = "Get all promotions",
        description = "Retrieves all available promotions from the system"
    )
    public ResponseEntity<List<PromotionResponseDTO>> getAllPromotions() {
        try {
            List<PromotionResponseDTO> promotions = promotionService.getAllPromotions();
            return ResponseEntity.ok(promotions);
        } catch (Exception e) {
            log.error("Failed to get promotions: {}", e.getMessage());
            throw new RuntimeException("Failed to get promotions: " + e.getMessage());
        }
    }
} 