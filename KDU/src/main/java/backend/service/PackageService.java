package backend.service;

import backend.dto.request.PackageSearchRequestDTO;
import backend.dto.response.RoomTypePackagesDTO;
import java.util.List;

public interface PackageService {
    List<RoomTypePackagesDTO> getAllPackages(PackageSearchRequestDTO searchRequest);
} 