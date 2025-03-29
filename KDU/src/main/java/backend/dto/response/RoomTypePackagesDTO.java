package backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class RoomTypePackagesDTO {
    private Long roomTypeId;
    private List<PackageResponseDTO> packages;
} 