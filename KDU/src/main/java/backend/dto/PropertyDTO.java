package backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyDTO {
    private Long id;
    private String name;
    private String address;

    public PropertyDTO() {
    }

    public PropertyDTO(Long id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }
}