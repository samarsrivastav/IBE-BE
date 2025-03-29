package backend.service;

import backend.dto.Request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;

public interface PromoCodeService {
    PromoCodeResponseDTO validateAndGetPromoCode(String name, PromoCodeValidationRequestDTO validationRequest);
    PromoCodeResponseDTO updatePromoCodeStatus(Long id, Boolean isActive);
} 