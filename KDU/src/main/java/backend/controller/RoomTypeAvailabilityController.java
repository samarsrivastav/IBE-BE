package backend.controller;

import backend.dto.request.RoomTypeSearchRequestDTO;
import backend.dto.response.RoomTypeResponseDTO;
import backend.service.RoomTypeAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/room-types")
@RequiredArgsConstructor
@Tag(name = "Room Type Availability", description = "APIs for searching available room types")
public class RoomTypeAvailabilityController {

    private final RoomTypeAvailabilityService roomTypeAvailabilityService;

    @PostMapping("/search")
    @Operation(
        summary = "Search available room types",
        description = "Search for available room types based on date range, capacity, and other criteria"
    )
    public ResponseEntity<List<RoomTypeResponseDTO>> searchAvailableRoomTypes(
            @Parameter(description = "Search criteria for room types")
            @Valid @RequestBody RoomTypeSearchRequestDTO searchRequest) {
        
        try {
            List<RoomTypeResponseDTO> availableRoomTypes = roomTypeAvailabilityService.getAvailableRoomTypes(searchRequest);
            return ResponseEntity.ok(availableRoomTypes);
        } catch (Exception e) {
            log.error("Failed to search available room types: {}", e.getMessage());
            throw new RuntimeException("Failed to search available room types: " + e.getMessage());
        }
    }
} 