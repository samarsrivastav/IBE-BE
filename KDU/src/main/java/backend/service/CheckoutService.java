package backend.service;

import backend.dto.request.CheckoutRequestDTO;
import backend.dto.response.CheckoutResponseDTO;

public interface CheckoutService {
    CheckoutResponseDTO createCheckout(CheckoutRequestDTO request);
    CheckoutResponseDTO getCheckoutById(Long id);
} 