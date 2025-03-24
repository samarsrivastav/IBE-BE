package backend.controller;

import backend.entity.CustomPromotion;
import backend.exception.TenantNotAllowedException;
import backend.service.CustomPromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomPromotionControllerTest {

    @Mock
    private CustomPromotionService service;

    @InjectMocks
    private CustomPromotionController controller;

    private CustomPromotion promotion;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        promotion = new CustomPromotion();
        promotion.setTenantId(1L);
        promotion.setStartDate(LocalDate.now());
        promotion.setEndDate(LocalDate.now().plusDays(7));
        promotion.setDiscount("10%");
    }

    @Test
    void createPromotion_ValidTenant_ShouldReturnPromotion() {
        when(service.createPromotion(any(CustomPromotion.class))).thenReturn(promotion);

        ResponseEntity<CustomPromotion> response = controller.createPromotion(promotion);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void createPromotion_InvalidTenant_ShouldThrowException() {
        promotion.setTenantId(2L);

        assertThrows(TenantNotAllowedException.class, () -> controller.createPromotion(promotion));
    }

    @Test
    void getPromotionsByTenant_ValidTenant_ShouldReturnPromotions() {
        when(service.getPromotionsByTenant(1L)).thenReturn(List.of(promotion));

        ResponseEntity<List<CustomPromotion>> response = controller.getPromotionsByTenant(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void deletePromotion_ValidTenant_ShouldReturnNoContent() {
        doNothing().when(service).deletePromotion(1L);

        ResponseEntity<Void> response = controller.deletePromotion(1L, 1L);

        assertEquals(204, response.getStatusCodeValue());
    }
}
