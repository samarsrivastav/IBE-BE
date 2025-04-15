package backend.controller;

import backend.dto.request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.service.PromoCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promo Code Management", description = "APIs for managing promotional codes")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PostMapping("/validate/{code}")
    @Operation(summary = "Validate and get promo code details",
            description = "Validates a promo code based on usage date and optionally promo type")
    public ResponseEntity<PromoCodeResponseDTO> validatePromoCode(
            @Parameter(description = "Promo code to validate", example = "NEW50")
            @PathVariable("code") String code,
            @Valid @RequestBody PromoCodeValidationRequestDTO validationRequest) {
        return ResponseEntity.ok(promoCodeService.validateAndGetPromoCode(code, validationRequest));
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle promo code status",
            description = "Activates or deactivates a promo code")
    public ResponseEntity<PromoCodeResponseDTO> togglePromoCodeStatus(
            @Parameter(description = "ID of the promo code", example = "1")
            @PathVariable Long id,
            @Parameter(description = "New status of the promo code", example = "true")
            @RequestParam Boolean isActive) {
        return ResponseEntity.ok(promoCodeService.updatePromoCodeStatus(id, isActive));
    }
}