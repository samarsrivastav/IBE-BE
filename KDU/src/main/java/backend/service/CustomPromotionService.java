package backend.service;

import backend.entity.CustomPromotion;
import backend.repository.CustomPromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomPromotionService {

    private final CustomPromotionRepository repository;

    public CustomPromotion createPromotion( CustomPromotion promotion) {
        return repository.save(promotion);
    }

    public List<CustomPromotion> getPromotionsByTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public void deletePromotion(Long id) {
        repository.deleteById(id);
    }
}
