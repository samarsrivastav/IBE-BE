package backend.controller;

import backend.entity.CustomPromotion;
import backend.service.CustomPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/custom-promotions")
public class CustomPromotionController {


    private final CustomPromotionService service;

    @PostMapping
    public ResponseEntity<CustomPromotion> createPromotion(@RequestBody CustomPromotion promotion) {
        return ResponseEntity.ok(service.createPromotion(promotion));
    }

    @GetMapping
    public ResponseEntity<List<CustomPromotion>> getAllPromotions() {
        return ResponseEntity.ok(service.getAllPromotions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomPromotion> getPromotionById(@PathVariable Long id) {
        Optional<CustomPromotion> promotion = service.getPromotionById(id);
        return promotion.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<CustomPromotion>> getPromotionsByTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(service.getPromotionsByTenant(tenantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        service.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
