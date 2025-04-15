package backend.service;

import backend.dto.request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.entity.PromoCode;
import java.time.LocalDate;
import java.util.List;

public interface PromoCodeService {
    PromoCodeResponseDTO validateAndGetPromoCode(String name, PromoCodeValidationRequestDTO validationRequest);
    PromoCodeResponseDTO updatePromoCodeStatus(Long id, Boolean isActive);

    /**
     * Get all active promo codes that are applicable for the given date range
     * @param startDate The start date of the stay
     * @param endDate The end date of the stay
     * @return List of applicable promo codes
     */
    List<PromoCode> getApplicablePromoCodes(LocalDate startDate, LocalDate endDate);
} 