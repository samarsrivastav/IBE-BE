package backend.controller;

import backend.dto.request.CheckoutRequestDTO;
import backend.dto.response.CheckoutResponseDTO;
import backend.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<CheckoutResponseDTO> createCheckout(@Valid @RequestBody CheckoutRequestDTO request) {
        log.info("Received checkout request for room type: {}", request.getRoomTypeId());
        CheckoutResponseDTO response = checkoutService.createCheckout(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckoutResponseDTO> getCheckout(@PathVariable Long id) {
        log.info("Fetching checkout with ID: {}", id);
        CheckoutResponseDTO response = checkoutService.getCheckoutById(id);
        return ResponseEntity.ok(response);
    }
} 