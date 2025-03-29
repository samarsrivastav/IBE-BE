package backend.dto.response;

import lombok.Data;

@Data
public class PackageResponseDTO {
    private String id;
    private String title;
    private String description;
    private String price;
    private String type;
} 