package backend.service;

import backend.entity.CustomPromotion;
import backend.repository.CustomPromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomPromotionService {

    private static final Logger log = LoggerFactory.getLogger(CustomPromotionService.class);
    private final CustomPromotionRepository repository;


    public CustomPromotion createPromotion( CustomPromotion promotion) {
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
}
