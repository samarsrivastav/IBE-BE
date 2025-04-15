package backend.service.impl;

import backend.dto.request.RoomTypeRatingRequestDTO;
import backend.entity.RoomTypes;
import backend.repository.RoomTypeRepository;
import backend.service.RoomTypeRatingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTypeRatingServiceImpl implements RoomTypeRatingService {

    private final RoomTypeRepository roomTypesRepository;

    @Override
    @Transactional
    public RoomTypes updateRating(RoomTypeRatingRequestDTO request) {
        log.info("Updating rating for room type {} with new rating {}", 
            request.getRoomTypeId(), request.getRating());

        // Validate rating
        if (request.getRating() < 1.0 || request.getRating() > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Get current room type
        RoomTypes roomType = roomTypesRepository.findById(request.getRoomTypeId())
            .orElseThrow(() -> new EntityNotFoundException("Room type not found with id: " + request.getRoomTypeId()));

        // Get current values
        double currentStars = roomType.getStars();
        int currentReviewCount = roomType.getNumberOfReviews();

        // Calculate new average
        double newStars = calculateNewAverage(currentStars, currentReviewCount, request.getRating());
        int newReviewCount = currentReviewCount + 1;

        // Update values
        roomType.setStars(newStars);
        roomType.setNumberOfReviews(newReviewCount);

        // Save and return
        RoomTypes updatedRoomType = roomTypesRepository.save(roomType);
        log.info("Updated room type rating - New average: {}, New review count: {}", 
            newStars, newReviewCount);

        return updatedRoomType;
    }

    private double calculateNewAverage(double currentAverage, int currentCount, double newRating) {
        // Formula: (currentAverage * currentCount + newRating) / (currentCount + 1)
        return (currentAverage * currentCount + newRating) / (currentCount + 1);
    }
} 