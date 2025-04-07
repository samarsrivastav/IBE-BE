package backend.service;

import backend.dto.request.RoomTypeRatingRequestDTO;
import backend.entity.RoomTypes;

public interface RoomTypeRatingService {
    /**
     * Update the average rating and review count for a room type
     * @param request The request containing room type ID and new rating
     * @return Updated room type with new average rating and review count
     */
    RoomTypes updateRating(RoomTypeRatingRequestDTO request);
} 