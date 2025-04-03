package backend.controller;

import backend.dto.request.RoomTypeRatingRequestDTO;
import backend.entity.RoomTypes;
import backend.service.RoomTypeRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room-types/ratings")
@RequiredArgsConstructor
public class RoomTypeRatingController {

    private final RoomTypeRatingService ratingService;

    @PostMapping
    public ResponseEntity<RoomTypes> updateRating(@RequestBody RoomTypeRatingRequestDTO request) {
        return ResponseEntity.ok(ratingService.updateRating(request));
    }
} 