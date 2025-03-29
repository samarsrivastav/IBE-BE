package backend.service.impl;

import backend.dto.Request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.entity.PromoCode;
import backend.repository.PromoCodeRepository;
import backend.service.PromoCodeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PromoCodeServiceImpl implements PromoCodeService {

    private static final Logger log = LoggerFactory.getLogger(PromoCodeServiceImpl.class);
    private final PromoCodeRepository promoCodeRepository;

    @Override
    public PromoCodeResponseDTO validateAndGetPromoCode(String name, PromoCodeValidationRequestDTO validationRequest) {
        PromoCode promoCode = promoCodeRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Promo code not found with name: " + name));

        // Log promo code details for debugging
        log.info("Found promo code: name={}, isActive={}, startDate={}, endDate={}, usageDate={}, promoType={}",
                promoCode.getName(),
                promoCode.getIsActive(),
                promoCode.getStartDate(),
                promoCode.getEndDate(),
                validationRequest.getUsageDate(),
                validationRequest.getPromoType());

        // Validate promo type only if provided in request
        if (validationRequest.getPromoType() != null && 
            !promoCode.getPromoType().equals(validationRequest.getPromoType())) {
            String errorMsg = String.format("Invalid promo type. Expected: %s, Provided: %s",
                    promoCode.getPromoType(), validationRequest.getPromoType());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Check if promo code is valid
        if (!isPromoCodeValid(promoCode, validationRequest.getUsageDate())) {
            String reason = getValidationFailureReason(promoCode, validationRequest.getUsageDate());
            log.error("Promo code validation failed: {}", reason);
            throw new IllegalStateException(reason);
        }

        return convertToResponseDTO(promoCode);
    }

    @Override
    @Transactional
    public PromoCodeResponseDTO updatePromoCodeStatus(Long id, Boolean isActive) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promo code not found with id: " + id));

        promoCode.setIsActive(isActive);
        PromoCode updatedPromoCode = promoCodeRepository.save(promoCode);
        return convertToResponseDTO(updatedPromoCode);
    }

    private boolean isPromoCodeValid(PromoCode promoCode, LocalDate usageDate) {
        return promoCode.getIsActive() && isWithinDateRange(promoCode, usageDate);
    }

    private boolean isWithinDateRange(PromoCode promoCode, LocalDate usageDate) {
        return !usageDate.isBefore(promoCode.getStartDate()) && !usageDate.isAfter(promoCode.getEndDate());
    }

    private String getValidationFailureReason(PromoCode promoCode, LocalDate usageDate) {
        if (!promoCode.getIsActive()) {
            return "Promo code is not active";
        }
        if (usageDate.isBefore(promoCode.getStartDate())) {
            return "Promo code has not started yet (starts on " + promoCode.getStartDate() + ")";
        }
        if (usageDate.isAfter(promoCode.getEndDate())) {
            return "Promo code has expired (ended on " + promoCode.getEndDate() + ")";
        }
        return "Unknown validation failure";
    }

    private PromoCodeResponseDTO convertToResponseDTO(PromoCode promoCode) {
        PromoCodeResponseDTO responseDTO = new PromoCodeResponseDTO();
        BeanUtils.copyProperties(promoCode, responseDTO);
        return responseDTO;
    }
} 