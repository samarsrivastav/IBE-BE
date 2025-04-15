package backend.service;

import backend.entity.CustomPromotion;
import backend.repository.CustomPromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomPromotionService {

    private static final Logger log = LoggerFactory.getLogger(CustomPromotionService.class);
    private final CustomPromotionRepository repository;

    public CustomPromotion createPromotion(CustomPromotion promotion) {
        log.info("Creating new promotion: {}", promotion);
        return repository.save(promotion);
    }

    public List<CustomPromotion> getPromotionsByTenant(Long tenantId) {
        log.info("Getting promotions for tenant: {}", tenantId);
        return repository.findByTenantId(tenantId);
    }

    public void deletePromotion(Long id) {
        log.info("Removing promotion for tenant: {}", id);
        repository.deleteById(id);
    }

    /**
     * Get all active promotions that are applicable for the given date range
     * @param startDate The start date of the stay
     * @param endDate The end date of the stay
     * @return List of applicable promotions
     */
    public List<CustomPromotion> getApplicablePromotions(LocalDate startDate, LocalDate endDate) {
        log.info("Getting applicable promotions for date range: {} to {}", startDate, endDate);
        List<CustomPromotion> allPromotions = repository.findByTenantId(1L);
        log.info("Found {} total promotions in database", allPromotions.size());
        
        List<CustomPromotion> applicablePromotions = allPromotions.stream()
            .filter(promotion -> {
                boolean isActive = promotion.isActive();
                // Check if there's any overlap between the date ranges
                boolean dateOverlap = !startDate.isAfter(promotion.getEndDate()) && 
                                    !endDate.isBefore(promotion.getStartDate());
                
                log.info("Checking promotion: {} ({} to {}) - Active: {}, Date Overlap: {}", 
                    promotion.getTitle(), 
                    promotion.getStartDate(), 
                    promotion.getEndDate(),
                    isActive,
                    dateOverlap);
                    
                if (dateOverlap) {
                    log.info("Date overlap found: User dates ({}-{}) overlap with promotion dates ({}-{})", 
                        startDate, endDate, promotion.getStartDate(), promotion.getEndDate());
                }
                
                return isActive && dateOverlap;
            })
            .peek(promotion -> log.info("Found applicable promotion: {} ({} to {})", 
                promotion.getTitle(), promotion.getStartDate(), promotion.getEndDate()))
            .toList();
            
        log.info("Found {} applicable promotions out of {} total promotions", 
            applicablePromotions.size(), allPromotions.size());
        return applicablePromotions;
    }
}
