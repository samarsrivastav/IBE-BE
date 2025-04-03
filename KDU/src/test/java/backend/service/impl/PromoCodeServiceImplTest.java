package backend.service.impl;

import backend.dto.request.PromoCodeValidationRequestDTO;
import backend.dto.response.PromoCodeResponseDTO;
import backend.entity.PromoCode;
import backend.entity.Enum.PromoType;
import backend.repository.PromoCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceImplTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PromoCodeServiceImpl promoCodeService;

    private PromoCode promoCode;
    private PromoCodeValidationRequestDTO validationRequest;
    private String promoCodeName;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        promoCodeName = "NEW50";

        promoCode = new PromoCode();
        promoCode.setId(1L);
        promoCode.setName(promoCodeName);
        promoCode.setTitle("New Year Special");
        promoCode.setDescription("50% off on all bookings");
        promoCode.setDiscount(50.0);
        promoCode.setIsActive(true);
        promoCode.setPromoType(PromoType.TIME_BASED);
        promoCode.setStartDate(today.minusDays(1));
        promoCode.setEndDate(today.plusDays(30));

        validationRequest = new PromoCodeValidationRequestDTO();
        validationRequest.setUsageDate(today);
        validationRequest.setPromoType(PromoType.TIME_BASED);
    }

    @Test
    void validateAndGetPromoCode_WithValidCode_ReturnsPromoCodeDetails() {
        // Arrange
        when(promoCodeRepository.findByName(promoCodeName)).thenReturn(Optional.of(promoCode));

        // Act
        PromoCodeResponseDTO result = promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest);

        // Assert
        assertNotNull(result);
        assertEquals(promoCodeName, result.getName());
        assertEquals(50.0, result.getDiscount());
        assertEquals(PromoType.TIME_BASED, result.getPromoType());
        verify(promoCodeRepository).findByName(promoCodeName);
    }

    @Test
    void validateAndGetPromoCode_WithNonExistentCode_ThrowsException() {
        // Arrange
        when(promoCodeRepository.findByName(promoCodeName)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
            () -> promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest));
        assertEquals("Promo code not found with name: " + promoCodeName, exception.getMessage());
        verify(promoCodeRepository).findByName(promoCodeName);
    }

    @Test
    void validateAndGetPromoCode_WithInactiveCode_ThrowsException() {
        // Arrange
        promoCode.setIsActive(false);
        when(promoCodeRepository.findByName(promoCodeName)).thenReturn(Optional.of(promoCode));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest));
        assertEquals("Promo code is not active", exception.getMessage());
        verify(promoCodeRepository).findByName(promoCodeName);
    }

    @Test
    void validateAndGetPromoCode_WithExpiredCode_ThrowsException() {
        // Arrange
        promoCode.setEndDate(today.minusDays(1));
        when(promoCodeRepository.findByName(promoCodeName)).thenReturn(Optional.of(promoCode));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest));
        assertTrue(exception.getMessage().contains("Promo code has expired"));
        verify(promoCodeRepository).findByName(promoCodeName);
    }

    @Test
    void validateAndGetPromoCode_WithFutureStartDate_ThrowsException() {
        // Arrange
        promoCode.setStartDate(today.plusDays(1));
        when(promoCodeRepository.findByName(promoCodeName)).thenReturn(Optional.of(promoCode));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest));
        assertTrue(exception.getMessage().contains("Promo code has not started yet"));
        verify(promoCodeRepository).findByName(promoCodeName);
    }

    @Test
    void validateAndGetPromoCode_WithInvalidPromoType_ThrowsException() {
        // Arrange
        validationRequest.setPromoType(PromoType.ONE_TIME_USE);
        when(promoCodeRepository.findByName(promoCodeName)).thenReturn(Optional.of(promoCode));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> promoCodeService.validateAndGetPromoCode(promoCodeName, validationRequest));
        assertTrue(exception.getMessage().contains("Invalid promo type"));
        verify(promoCodeRepository).findByName(promoCodeName);
    }

    @Test
    void updatePromoCodeStatus_WithValidId_ReturnsUpdatedPromoCode() {
        // Arrange
        Long promoCodeId = 1L;
        Boolean isActive = true;
        when(promoCodeRepository.findById(promoCodeId)).thenReturn(Optional.of(promoCode));
        when(promoCodeRepository.save(any(PromoCode.class))).thenReturn(promoCode);

        // Act
        PromoCodeResponseDTO result = promoCodeService.updatePromoCodeStatus(promoCodeId, isActive);

        // Assert
        assertNotNull(result);
        assertEquals(promoCodeName, result.getName());
        assertEquals(50.0, result.getDiscount());
        verify(promoCodeRepository).findById(promoCodeId);
        verify(promoCodeRepository).save(any(PromoCode.class));
    }

    @Test
    void updatePromoCodeStatus_WithNonExistentId_ThrowsException() {
        // Arrange
        Long promoCodeId = 999L;
        Boolean isActive = true;
        when(promoCodeRepository.findById(promoCodeId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
            () -> promoCodeService.updatePromoCodeStatus(promoCodeId, isActive));
        assertEquals("Promo code not found with id: " + promoCodeId, exception.getMessage());
        verify(promoCodeRepository).findById(promoCodeId);
        verify(promoCodeRepository, never()).save(any(PromoCode.class));
    }
} 