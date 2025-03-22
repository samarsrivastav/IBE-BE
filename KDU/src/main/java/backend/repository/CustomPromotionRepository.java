package backend.repository;

import backend.entity.CustomPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomPromotionRepository extends JpaRepository<CustomPromotion, Long> {
    List<CustomPromotion> findByTenantId(Long tenantId);
}
