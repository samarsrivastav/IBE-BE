package backend.service;

import backend.dto.response.PromotionResponseDTO;
import backend.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionService = mock(PromotionService.class);
    }

    @Test
    void testGetAllPromotions() {
        // Arrange
        PromotionResponseDTO promo1 = new PromotionResponseDTO();
        PromotionResponseDTO promo2 = new PromotionResponseDTO();
        List<PromotionResponseDTO> mockResponse = Arrays.asList(promo1, promo2);

        when(promotionService.getAllPromotions()).thenReturn(mockResponse);

        // Act
        List<PromotionResponseDTO> result = promotionService.getAllPromotions();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(promotionService, times(1)).getAllPromotions();
    }
}

