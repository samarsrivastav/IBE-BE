package backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "configuration")
@Getter
@Setter
@NoArgsConstructor
public class PropertyConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Integer propertyId;

    @Column( columnDefinition = "boolean default true")
    private boolean showGuest = true;

    @Column( columnDefinition = "boolean default false")
    private boolean wheelChairOption = false;

    @Column( columnDefinition = "boolean default true")
    private boolean showRoomNumber = true;

    @Column( columnDefinition = "integer default 4")
    private Integer maxGuestPerRoom = 4;

    @Column(columnDefinition = "jsonb default '{\"adult\": 18, \"child\": 12}'")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode guestTypes;


}

