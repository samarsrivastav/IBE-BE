package backend.controller;

import backend.dto.request.DailyRateRequestDTO;
import backend.dto.response.DailyRateResponseDTO;
import backend.service.DailyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily-rates")
@RequiredArgsConstructor
public class DailyRateController {

    private final DailyRateService dailyRateService;

    @PostMapping("/calculate")
    public ResponseEntity<List<DailyRateResponseDTO>> calculateDailyRates(
            @RequestBody DailyRateRequestDTO request) {
        return ResponseEntity.ok(dailyRateService.calculateDailyRates(request));
    }
} 