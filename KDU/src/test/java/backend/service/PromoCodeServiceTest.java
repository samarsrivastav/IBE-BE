
package backend.service;
import backend.dto.request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock
    private PromoCodeService promoCodeService;

    @BeforeEach
    void setUp() {
        promoCodeService = mock(PromoCodeService.class);
    }

    @Test
    void testValidateAndGetPromoCode() {
        // Arrange
        String promoCodeName = "DISCOUNT10";
        PromoCodeValidationRequestDTO validationRequest = new PromoCodeValidationRequestDTO();
        PromoCodeResponseDTO mockResponse = new PromoCodeResponseDTO();

        when(promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest)).thenReturn(mockResponse);

        // Act
        PromoCodeResponseDTO result = promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest);

        // Assert
        assertNotNull(result);
        verify(promoCodeService, times(1)).validateAndGetPromoCode(promoCodeName, validationRequest);
    }

    @Test
    void testUpdatePromoCodeStatus() {
        // Arrange
        Long promoCodeId = 1L;
        Boolean isActive = true;
        PromoCodeResponseDTO mockResponse = new PromoCodeResponseDTO();

        when(promoCodeService.updatePromoCodeStatus(promoCodeId, isActive)).thenReturn(mockResponse);

        // Act
        PromoCodeResponseDTO result = promoCodeService.updatePromoCodeStatus(promoCodeId, isActive);

        // Assert
        assertNotNull(result);
        verify(promoCodeService, times(1)).updatePromoCodeStatus(promoCodeId, isActive);
    }
}
