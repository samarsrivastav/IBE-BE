package backend.service;

import backend.entity.CustomPromotion;
import backend.repository.CustomPromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomPromotionServiceTest {

    @Mock
    private CustomPromotionRepository repository;

    @InjectMocks
    private CustomPromotionService service;

    private CustomPromotion promotion;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        promotion = new CustomPromotion();
        promotion.setPromotionId(1L);
        promotion.setTenantId(1L);
        promotion.setStartDate(LocalDate.now());
        promotion.setEndDate(LocalDate.now().plusDays(7));
        promotion.setDiscount(10);
    }

    @Test
    void createPromotion_ShouldSaveAndReturnPromotion() {
        when(repository.save(promotion)).thenReturn(promotion);

        CustomPromotion savedPromotion = service.createPromotion(promotion);

        assertNotNull(savedPromotion);
        assertEquals(1L, savedPromotion.getPromotionId());
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void getPromotionsByTenant_ShouldReturnList() {
        when(repository.findByTenantId(1L)).thenReturn(List.of(promotion));

        List<CustomPromotion> promotions = service.getPromotionsByTenant(1L);

        assertFalse(promotions.isEmpty());
        assertEquals(1, promotions.size());
        verify(repository, times(1)).findByTenantId(1L);
    }

    @Test
    void deletePromotion_ShouldCallRepositoryDelete() {
        doNothing().when(repository).deleteById(1L);

        service.deletePromotion(1L);

        verify(repository, times(1)).deleteById(1L);
    }
}
