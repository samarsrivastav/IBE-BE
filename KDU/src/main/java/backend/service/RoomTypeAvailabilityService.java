package backend.service;

import backend.dto.request.RoomTypeSearchRequestDTO;
import backend.dto.response.RoomTypeResponseDTO;

import java.util.List;
import java.util.Map;

public interface RoomTypeAvailabilityService {
    List<RoomTypeResponseDTO> getAvailableRoomTypes(RoomTypeSearchRequestDTO searchRequest);
    
    /**
     * Get the room rate for a specific room type
     * @param roomTypeId The ID of the room type
     * @return The room rate for the specified room type
     */
    double getRoomTypeRate(Long roomTypeId);
    Map<Long, Double> getAverageRates();

} 