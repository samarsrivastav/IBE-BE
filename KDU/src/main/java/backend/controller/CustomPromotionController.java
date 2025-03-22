package backend.controller;

import backend.entity.CustomPromotion;
import backend.exception.TenantNotAllowedException;
import backend.service.CustomPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/custom-promotions")
public class CustomPromotionController {

    private final CustomPromotionService service;

    @PostMapping
    public ResponseEntity<CustomPromotion> createPromotion( @RequestBody CustomPromotion promotion) {
        if (promotion.getTenantId() == null) {
            return ResponseEntity.badRequest().body(promotion);
        }
        validateTenant(promotion.getTenantId());
        return ResponseEntity.ok(service.createPromotion(promotion));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<List<CustomPromotion>> getPromotionsByTenant(@PathVariable Long tenantId) {
        validateTenant(tenantId);
        return ResponseEntity.ok(service.getPromotionsByTenant(tenantId));
    }

    @DeleteMapping("/{tenantId}/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long tenantId, @PathVariable Long id) {
        validateTenant(tenantId);
        service.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    private void validateTenant(Long tenantId) {
        if (tenantId!=1L) {
            throw new TenantNotAllowedException("Access denied for tenant: " + tenantId);
        }
    }
}
