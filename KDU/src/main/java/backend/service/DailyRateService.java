package backend.service;

import backend.dto.request.DailyRateRequestDTO;
import backend.dto.response.DailyRateResponseDTO;
import java.time.LocalDate;
import java.util.List;

public interface DailyRateService {
    /**
     * Calculate daily rates for a room type within a date range
     * @param request The request containing room type ID, date range, and optional promotion title
     * @return List of daily rates with applied discounts if applicable
     */
    List<DailyRateResponseDTO> calculateDailyRates(DailyRateRequestDTO request);
} 