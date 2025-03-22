package backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyDTO {
    // Getters and Setters
    private Long id;
    private String name;
    private String address;

    // Constructors
    public PropertyDTO() {
    }

    public PropertyDTO(Long id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

}