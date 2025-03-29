package backend.service;

import backend.dto.response.PromotionResponseDTO;
import java.util.List;

public interface PromotionService {
    List<PromotionResponseDTO> getAllPromotions();
} 