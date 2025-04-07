package backend.controller;

import backend.dto.request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.entity.Enum.PromoType;
import backend.service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeControllerTest {

    @Mock
    private PromoCodeService promoCodeService;

    @InjectMocks
    private PromoCodeController controller;

    private PromoCodeValidationRequestDTO validationRequest;
    private PromoCodeResponseDTO responseDTO;
    private String promoCode;

    @BeforeEach
    void setUp() {
        promoCode = "NEW50";
        
        validationRequest = new PromoCodeValidationRequestDTO();
        validationRequest.setUsageDate(LocalDate.now());
        validationRequest.setPromoType(PromoType.TIME_BASED);

        responseDTO = new PromoCodeResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName(promoCode);
        responseDTO.setTitle("New Year Special");
        responseDTO.setDescription("50% off on all bookings");
        responseDTO.setDiscount(50.0);
        responseDTO.setPromoType(PromoType.TIME_BASED);
    }

    @Test
    void validatePromoCode_WithValidCode_ReturnsPromoCodeDetails() {
        // Arrange
        when(promoCodeService.validateAndGetPromoCode(any(), any())).thenReturn(responseDTO);

        // Act
        ResponseEntity<PromoCodeResponseDTO> response = controller.validatePromoCode(promoCode, validationRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(promoCode, response.getBody().getName());
        assertEquals(50.0, response.getBody().getDiscount());
        verify(promoCodeService).validateAndGetPromoCode(promoCode, validationRequest);
    }

    @Test
    void validatePromoCode_WithInvalidCode_ThrowsException() {
        // Arrange
        when(promoCodeService.validateAndGetPromoCode(any(), any()))
            .thenThrow(new IllegalStateException("Promo code is not active"));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> controller.validatePromoCode(promoCode, validationRequest));
        assertEquals("Promo code is not active", exception.getMessage());
        verify(promoCodeService).validateAndGetPromoCode(promoCode, validationRequest);
    }

    @Test
    void togglePromoCodeStatus_WithValidId_ReturnsUpdatedPromoCode() {
        // Arrange
        Long promoCodeId = 1L;
        Boolean isActive = true;
        when(promoCodeService.updatePromoCodeStatus(any(), any())).thenReturn(responseDTO);

        // Act
        ResponseEntity<PromoCodeResponseDTO> response = controller.togglePromoCodeStatus(promoCodeId, isActive);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(promoCode, response.getBody().getName());
        verify(promoCodeService).updatePromoCodeStatus(promoCodeId, isActive);
    }

    @Test
    void togglePromoCodeStatus_WithInvalidId_ThrowsException() {
        // Arrange
        Long promoCodeId = 999L;
        Boolean isActive = true;
        when(promoCodeService.updatePromoCodeStatus(any(), any()))
            .thenThrow(new IllegalStateException("Promo code not found"));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> controller.togglePromoCodeStatus(promoCodeId, isActive));
        assertEquals("Promo code not found", exception.getMessage());
        verify(promoCodeService).updatePromoCodeStatus(promoCodeId, isActive);
    }


} 