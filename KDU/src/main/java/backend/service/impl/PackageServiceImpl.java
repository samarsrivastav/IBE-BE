package backend.service.impl;

import backend.dto.request.PackageSearchRequestDTO;
import backend.dto.request.RoomTypeSearchRequestDTO;
import backend.dto.response.PackageResponseDTO;
import backend.dto.response.RoomTypePackagesDTO;
import backend.dto.response.PromotionResponseDTO;
import backend.entity.CustomPromotion;
import backend.service.CustomPromotionService;
import backend.service.PackageService;
import backend.service.PromotionService;
import backend.service.RoomTypeAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final PromotionService promotionService;
    private final CustomPromotionService customPromotionService;
    private final RoomTypeAvailabilityService roomTypeAvailabilityService;

    private static final int MIN_STAY_DURATION = 1;

    @Override
    public List<RoomTypePackagesDTO> getAllPackages(PackageSearchRequestDTO searchRequest) {
        try {
            validateRequest(searchRequest);
            log.info("Processing package request for dates: {} to {}", 
                searchRequest.getStartDate(), searchRequest.getEndDate());
            
            // Get all room type rates
            Map<Long, Double> roomTypeRates = roomTypeAvailabilityService.getAverageRates();
            if (roomTypeRates.isEmpty()) {
                log.warn("No room types found for property {}", searchRequest.getPropertyId());
                return Collections.emptyList();
            }
            log.info("Found {} room types with rates", roomTypeRates.size());
            
            // Get all promotions
            List<PromotionResponseDTO> promotions = promotionService.getAllPromotions();
            log.info("Found {} promotions", promotions.size());

            // Get custom promotions
            List<CustomPromotion> customPromotions = customPromotionService.getApplicablePromotions(
                searchRequest.getStartDate(),
                searchRequest.getEndDate()
            );
            log.info("Found {} applicable custom promotions", customPromotions.size());
            if (!customPromotions.isEmpty()) {
                customPromotions.forEach(promo -> 
                    log.info("Applicable custom promotion: {} ({} to {}) with discount {}%", 
                        promo.getTitle(), 
                        promo.getStartDate(), 
                        promo.getEndDate(),
                        promo.getDiscount()));
            }

            // Create packages for each room type
            return roomTypeRates.keySet().stream()
                .map(roomTypeId -> createRoomTypePackages(roomTypeId, roomTypeRates.get(roomTypeId), searchRequest, promotions, customPromotions))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get packages: {}", e.getMessage());
            throw new RuntimeException("Failed to get packages", e);
        }
    }

    private RoomTypePackagesDTO createRoomTypePackages(Long roomTypeId, double roomTypeRate, PackageSearchRequestDTO searchRequest, 
            List<PromotionResponseDTO> promotions, List<CustomPromotion> customPromotions) {
        RoomTypePackagesDTO roomTypePackages = new RoomTypePackagesDTO();
        roomTypePackages.setRoomTypeId(roomTypeId);
        List<PackageResponseDTO> packages = new ArrayList<>();

        // Add standard rate package
        packages.add(createStandardRatePackage(roomTypeId, roomTypeRate));

        // Add weekend package if applicable
        if (isWeekendStay(searchRequest.getStartDate(), searchRequest.getEndDate(), 
                promotions.stream().filter(p -> p.getPromotionTitle().equals("Weekend discount"))
                    .findFirst().get().getMinimumDaysOfStay())) {
            promotions.stream()
                .filter(p -> p.getPromotionTitle().equals("Weekend discount"))
                .findFirst()
                .ifPresent(promotion -> packages.add(createWeekendPackage(roomTypeId, roomTypeRate, promotion)));
        }

        // Add long weekend package if applicable
        if (isLongWeekendStay(searchRequest.getStartDate(), searchRequest.getEndDate(),
                promotions.stream().filter(p -> p.getPromotionTitle().equals("Weekend discount"))
                    .findFirst().get().getMinimumDaysOfStay())) {
            promotions.stream()
                .filter(p -> p.getPromotionTitle().equals("Long weekend discount"))
                .findFirst()
                .ifPresent(promotion -> packages.add(createLongWeekendPackage(roomTypeId, roomTypeRate, promotion)));
        }

        // Add other promotions
        promotions.stream()
            .filter(p -> !p.getPromotionTitle().equals("Weekend discount") && 
                       !p.getPromotionTitle().equals("Long weekend discount"))
            .forEach(promotion -> packages.add(createPromotionPackage(roomTypeId, roomTypeRate, promotion)));

        // Add custom promotions
        log.info("Processing {} custom promotions for room type {}", customPromotions.size(), roomTypeId);
        customPromotions.forEach(promotion -> {
            PackageResponseDTO packageDTO = createCustomPromotionPackage(roomTypeId, roomTypeRate, promotion, searchRequest);
            packages.add(packageDTO);
            log.info("Added custom promotion package: {} with price {} (original discount: {}%)", 
                packageDTO.getTitle(), 
                packageDTO.getPrice(),
                promotion.getDiscount());
        });

        roomTypePackages.setPackages(packages);
        return roomTypePackages;
    }

    private PackageResponseDTO createStandardRatePackage(Long roomTypeId, double roomTypeRate) {
        PackageResponseDTO packageDTO = new PackageResponseDTO();
        packageDTO.setId("standard-" + roomTypeId);
        packageDTO.setTitle("Standard Rate");
        packageDTO.setDescription("Standard room rate with basic amenities");
        packageDTO.setType("standard");
        packageDTO.setPrice(String.valueOf(roundToTwoDecimals(roomTypeRate)));
        return packageDTO;
    }

    private PackageResponseDTO createWeekendPackage(Long roomTypeId, double roomTypeRate, PromotionResponseDTO promotion) {
        PackageResponseDTO packageDTO = new PackageResponseDTO();
        packageDTO.setId("weekend-" + roomTypeId);
        packageDTO.setTitle(promotion.getPromotionTitle());
        packageDTO.setDescription(promotion.getPromotionDescription());
        packageDTO.setType("package");
        packageDTO.setPrice(String.valueOf(roundToTwoDecimals(roomTypeRate * promotion.getPriceFactor())));
        return packageDTO;
    }

    private PackageResponseDTO createLongWeekendPackage(Long roomTypeId, double roomTypeRate, PromotionResponseDTO promotion) {
        PackageResponseDTO packageDTO = new PackageResponseDTO();
        packageDTO.setId("long-weekend-" + roomTypeId);
        packageDTO.setTitle(promotion.getPromotionTitle());
        packageDTO.setDescription(promotion.getPromotionDescription());
        packageDTO.setType("package");
        packageDTO.setPrice(String.valueOf(roundToTwoDecimals(roomTypeRate * promotion.getPriceFactor())));
        return packageDTO;
    }

    private PackageResponseDTO createPromotionPackage(Long roomTypeId, double roomTypeRate, PromotionResponseDTO promotion) {
        PackageResponseDTO packageDTO = new PackageResponseDTO();
        packageDTO.setId("promotion-" + promotion.getPromotionId() + "-" + roomTypeId);
        packageDTO.setTitle(promotion.getPromotionTitle());
        packageDTO.setDescription(promotion.getPromotionDescription());
        packageDTO.setType("package");
        packageDTO.setPrice(String.valueOf(roundToTwoDecimals(roomTypeRate * promotion.getPriceFactor())));
        return packageDTO;
    }

    private PackageResponseDTO createCustomPromotionPackage(Long roomTypeId, double roomTypeRate, 
            CustomPromotion promotion, PackageSearchRequestDTO searchRequest) {
        PackageResponseDTO packageDTO = new PackageResponseDTO();
        packageDTO.setId("custom-" + promotion.getPromotionId() + "-" + roomTypeId);
        packageDTO.setTitle(promotion.getTitle());
        
        // Calculate the number of days within promotion period
        LocalDate effectiveStartDate = searchRequest.getStartDate().isAfter(promotion.getStartDate()) 
            ? searchRequest.getStartDate() 
            : promotion.getStartDate();
        LocalDate effectiveEndDate = searchRequest.getEndDate().isBefore(promotion.getEndDate()) 
            ? searchRequest.getEndDate() 
            : promotion.getEndDate();
        
        long promotionDays = ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1;
        long totalDays = ChronoUnit.DAYS.between(searchRequest.getStartDate(), searchRequest.getEndDate()) + 1;
        
        // Calculate weighted average price
        double discountedRate = roomTypeRate * (1 - (double)(promotion.getDiscount())/100);
        double weightedPrice = ((promotionDays * discountedRate) + 
                              ((totalDays - promotionDays) * roomTypeRate)) / totalDays;
        
        packageDTO.setDescription(promotion.getDescription() + 
            String.format(" (Applicable for %d days)", promotionDays));
        packageDTO.setType("package");
        packageDTO.setPrice(String.valueOf(roundToTwoDecimals(weightedPrice)));
        return packageDTO;
    }

    private void validateRequest(PackageSearchRequestDTO request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        long stayDuration = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (stayDuration < MIN_STAY_DURATION) {
            throw new IllegalArgumentException("Minimum stay duration is " + MIN_STAY_DURATION + " day");
        }
    }

    private boolean isWeekendStay(LocalDate startDate, LocalDate endDate, int minimumStay) {
        long stayDuration = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (stayDuration < minimumStay) {
            return false;
        }
        return startDate.datesUntil(endDate.plusDays(1))
            .anyMatch(date -> date.getDayOfWeek() == DayOfWeek.SATURDAY || 
                            date.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    private boolean isLongWeekendStay(LocalDate startDate, LocalDate endDate, int minimumStay) {
        long stayDuration = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (stayDuration < minimumStay) {
            return false;
        }

        Set<DayOfWeek> weekendDays = startDate.datesUntil(endDate.plusDays(1))
            .map(LocalDate::getDayOfWeek)
            .filter(day -> day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
            .collect(Collectors.toSet());

        return weekendDays.contains(DayOfWeek.SATURDAY) && 
               weekendDays.contains(DayOfWeek.SUNDAY);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
} 