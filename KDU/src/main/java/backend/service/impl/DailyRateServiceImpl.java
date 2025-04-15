package backend.service.impl;

import backend.dto.request.DailyRateRequestDTO;
import backend.dto.response.DailyRateResponseDTO;
import backend.service.DailyRateService;
import backend.service.RoomTypeAvailabilityService;
import backend.service.CustomPromotionService;
import backend.service.PromoCodeService;
import backend.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRateServiceImpl implements DailyRateService {

    private final RoomTypeAvailabilityService roomTypeAvailabilityService;
    private final CustomPromotionService customPromotionService;
    private final PromoCodeService promoCodeService;
    private final PromotionService promotionService;

    @Override
    public List<DailyRateResponseDTO> calculateDailyRates(DailyRateRequestDTO request) {
        log.info("Calculating daily rates for room type {} between {} and {}", 
            request.getRoomTypeId(), request.getStartDate(), request.getEndDate());
        
        // Get rates from RoomTypeAvailabilityService
        Map<Long, List<Double>> ratesByRoomType = roomTypeAvailabilityService.getRatesByRoomType();

        List<Double> roomTypeRates = ratesByRoomType.getOrDefault(request.getRoomTypeId(), new ArrayList<>());
        log.info("Found {} rates for room type {}", roomTypeRates.size(), request.getRoomTypeId());
        
        // Get all promotions, custom promotions, and promo codes
        var standardPromotions = promotionService.getAllPromotions();
        log.info("Found {} standard promotions", standardPromotions.size());
        
        var customPromotions = customPromotionService.getApplicablePromotions(
            request.getStartDate(), 
            request.getEndDate()
        );
        log.info("Found {} custom promotions", customPromotions.size());
        
        var promoCodes = promoCodeService.getApplicablePromoCodes(
            request.getStartDate(), 
            request.getEndDate()
        );
        log.info("Found {} promo codes", promoCodes.size());
        
        // Find matching promotion by title if provided

        var matchingStandardPromotion = request.getPromotionTitle() != null ? 
            standardPromotions.stream()
                .filter(promo -> promo.getPromotionTitle().equals(request.getPromotionTitle()))
                .findFirst()
                .orElse(null) : null;

        var matchingCustomPromotion = request.getPromotionTitle() != null ? 
            customPromotions.stream()
                .filter(promo -> promo.getTitle().equals(request.getPromotionTitle()))
                .findFirst()
                .orElse(null) : null;
                
        var matchingPromoCode = request.getPromotionTitle() != null ? 
            promoCodes.stream()
                .filter(promo -> promo.getTitle().equals(request.getPromotionTitle()))
                .findFirst()
                .orElse(null) : null;
        
        // Calculate daily rates
        List<DailyRateResponseDTO> dailyRates = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        
        for (int i = 0; i < days; i++) {
            LocalDate currentDate = request.getStartDate().plusDays(i);
            BigDecimal baseRate = BigDecimal.valueOf(roomTypeRates.get(i));
            
            DailyRateResponseDTO dailyRate = new DailyRateResponseDTO();
            dailyRate.setDate(currentDate);
            dailyRate.setDay(currentDate.getDayOfWeek());
            dailyRate.setOriginalRate(baseRate);
            
            // Apply discount only if there's an exact title match
            if (matchingStandardPromotion != null && !matchingStandardPromotion.getIsDeactivated()) {
                
                // Apply standard promotion discount
                BigDecimal discountAmount = baseRate.multiply(BigDecimal.valueOf((matchingStandardPromotion.getPriceFactor())));
                BigDecimal discountedRate = baseRate.subtract(discountAmount);
                

                dailyRate.setDiscountAmount(discountAmount);

                
                log.debug("Applied standard promotion discount for {}: Original={}, Discounted={}, Discount={}", 
                    currentDate, baseRate, discountedRate, discountAmount);
            } 
            else if (matchingCustomPromotion != null && 
                !currentDate.isBefore(matchingCustomPromotion.getStartDate()) && 
                !currentDate.isAfter(matchingCustomPromotion.getEndDate())) {
                
                // Apply custom promotion discount
                BigDecimal discountAmount = baseRate.multiply(BigDecimal.valueOf(100 - matchingCustomPromotion.getDiscount()))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal discountedRate = baseRate.subtract(discountAmount);
                

                dailyRate.setDiscountAmount(discountAmount);

                
                log.debug("Applied custom promotion discount for {}: Original={}, Discounted={}, Discount={}", 
                    currentDate, baseRate, discountedRate, discountAmount);
            } 
            else if (matchingPromoCode != null && matchingPromoCode.getIsActive()) {
                
                // Apply promo code discount
                BigDecimal discountAmount = baseRate.multiply(BigDecimal.valueOf(100 - matchingPromoCode.getDiscount()))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal discountedRate = baseRate.subtract(discountAmount);
                dailyRate.setDiscountAmount(discountAmount);

                
                log.debug("Applied promo code discount for {}: Original={}, Discounted={}, Discount={}", 
                    currentDate, baseRate, discountedRate, discountAmount);
            } else {
                // No matching promotion found, use standard rate
                dailyRate.setDiscountAmount(baseRate);
                
                log.debug("No matching promotion for {}: Using standard rate={}", currentDate, baseRate);
            }
            
            dailyRates.add(dailyRate);
        }
        
        log.info("Calculated {} daily rates", dailyRates.size());
        return dailyRates;
    }
} 