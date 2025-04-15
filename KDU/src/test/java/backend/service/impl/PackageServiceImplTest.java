package backend.service.impl;

import backend.dto.request.PackageSearchRequestDTO;
import backend.dto.response.PromotionResponseDTO;
import backend.entity.CustomPromotion;
import backend.repository.CustomPromotionRepository;
import backend.service.CustomPromotionService;
import backend.service.PromotionService;
import backend.service.RoomTypeAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceImplTest {

    @Mock
    private PromotionService promotionService;

    @Mock
    private CustomPromotionService customPromotionService;

    @Mock
    private RoomTypeAvailabilityService roomTypeAvailabilityService;

    @Mock
    private CustomPromotionRepository customPromotionRepository;

    @InjectMocks
    private PackageServiceImpl packageService;

    private PackageSearchRequestDTO searchRequest;
    private Map<Long, Double> roomTypeRates;
    private List<PromotionResponseDTO> promotions;
    private List<CustomPromotion> customPromotions;

    @BeforeEach
    void setUp() {
        searchRequest = new PackageSearchRequestDTO();
        searchRequest.setPropertyId(1L);
        searchRequest.setStartDate(LocalDate.now());
        searchRequest.setEndDate(LocalDate.now().plusDays(2));

        roomTypeRates = new HashMap<>();
        roomTypeRates.put(1L, 100.0);
        roomTypeRates.put(2L, 200.0);

        promotions = Arrays.asList(
            createPromotionResponseDTO(1L, "Weekend discount", "Weekend special", 0.9),
            createPromotionResponseDTO(2L, "Long weekend discount", "Long weekend special", 0.85),
            createPromotionResponseDTO(3L, "Summer special", "Summer discount", 0.95)
        );

        customPromotions = Arrays.asList(
            createCustomPromotion(1L, "Early bird", "Book early", 10, LocalDate.now(), LocalDate.now().plusDays(30)),
            createCustomPromotion(2L, "Last minute", "Last minute deals", 15, LocalDate.now(), LocalDate.now().plusDays(30))
        );
    }

    @Test
    void getAllPackages_WithValidRequest_ReturnsPackages() {
        // Arrange
        when(roomTypeAvailabilityService.getAverageRates()).thenReturn(roomTypeRates);
        when(promotionService.getAllPromotions()).thenReturn(promotions);
        when(customPromotionService.getApplicablePromotions(any(), any())).thenReturn(customPromotions);

        // Act
        var result = packageService.getAllPackages(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // One for each room type
        verify(roomTypeAvailabilityService).getAverageRates();
        verify(promotionService).getAllPromotions();
        verify(customPromotionService).getApplicablePromotions(any(), any());
    }

    @Test
    void getAllPackages_WithNoRoomTypes_ReturnsEmptyList() {
        // Arrange
        when(roomTypeAvailabilityService.getAverageRates()).thenReturn(Collections.emptyMap());

        // Act
        var result = packageService.getAllPackages(searchRequest);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllPackages_WithInvalidDates_ThrowsException() {
        // Arrange
        searchRequest.setEndDate(searchRequest.getStartDate().minusDays(1));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> packageService.getAllPackages(searchRequest));
        assertEquals("Failed to get packages", exception.getMessage());
    }

    @Test
    void getAllPackages_WithServiceException_ThrowsRuntimeException() {
        // Arrange
        when(roomTypeAvailabilityService.getAverageRates()).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> packageService.getAllPackages(searchRequest));
        assertEquals("Failed to get packages", exception.getMessage());
    }

    private PromotionResponseDTO createPromotionResponseDTO(Long id, String title, String description, double priceFactor) {
        PromotionResponseDTO dto = new PromotionResponseDTO();
        dto.setPromotionId(id);
        dto.setPromotionTitle(title);
        dto.setPromotionDescription(description);
        dto.setPriceFactor(priceFactor);
        dto.setMinimumDaysOfStay(1);
        return dto;
    }

    private CustomPromotion createCustomPromotion(Long id, String title, String description, 
            int discount, LocalDate startDate, LocalDate endDate) {
        CustomPromotion promotion = new CustomPromotion();
        promotion.setPromotionId(id);
        promotion.setTitle(title);
        promotion.setDescription(description);
        promotion.setDiscount(discount);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setTenantId(1L);
        promotion.setActive(true);
        return promotion;
    }
} 