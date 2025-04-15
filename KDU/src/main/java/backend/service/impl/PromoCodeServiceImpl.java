package backend.service.impl;

import backend.dto.request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.entity.PromoCode;
import backend.repository.PromoCodeRepository;
import backend.service.PromoCodeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeServiceImpl implements PromoCodeService {

    private final PromoCodeRepository repository;

    @Override
    public List<PromoCode> getApplicablePromoCodes(LocalDate startDate, LocalDate endDate) {
        log.info("Getting applicable promo codes for end date: {}", endDate);
        List<PromoCode> allPromoCodes = repository.findAll();
        log.info("Found {} total promo codes in database", allPromoCodes.size());
        
        List<PromoCode> applicablePromoCodes = allPromoCodes.stream()
            .filter(promoCode -> {
                // Only check if the end date falls within the promo code's date range
                boolean dateInRange = !endDate.isBefore(promoCode.getStartDate()) && 
                                    !endDate.isAfter(promoCode.getEndDate());
                
                log.info("Checking promo code: {} ({} to {}) - End Date In Range: {}", 
                    promoCode.getName(), 
                    promoCode.getStartDate(), 
                    promoCode.getEndDate(),
                    dateInRange);
                    
                if (dateInRange) {
                    log.info("End date {} falls within promo code date range ({}-{})", 
                        endDate, promoCode.getStartDate(), promoCode.getEndDate());
                }
                
                return dateInRange;
            })
            .peek(promoCode -> log.info("Found applicable promo code: {} ({} to {})", 
                promoCode.getName(), promoCode.getStartDate(), promoCode.getEndDate()))
            .toList();
            
        log.info("Found {} applicable promo codes out of {} total promo codes", 
            applicablePromoCodes.size(), allPromoCodes.size());
        return applicablePromoCodes;
    }

    @Override
    public PromoCodeResponseDTO validateAndGetPromoCode(String name, PromoCodeValidationRequestDTO validationRequest) {
        PromoCode promoCode = repository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Promo code not found with name: " + name));

        // Log promo code details for debugging
        log.info("Found promo code: name={}, startDate={}, endDate={}, usageDate={}, promoType={}",
                promoCode.getName(),
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
        PromoCode promoCode = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promo code not found with id: " + id));

        promoCode.setIsActive(isActive);
        PromoCode updatedPromoCode = repository.save(promoCode);
        return convertToResponseDTO(updatedPromoCode);
    }

    private boolean isPromoCodeValid(PromoCode promoCode, LocalDate usageDate) {
        return isWithinDateRange(promoCode, usageDate);
    }

    private boolean isWithinDateRange(PromoCode promoCode, LocalDate usageDate) {
        return !usageDate.isBefore(promoCode.getStartDate()) && !usageDate.isAfter(promoCode.getEndDate());
    }

    private String getValidationFailureReason(PromoCode promoCode, LocalDate usageDate) {
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