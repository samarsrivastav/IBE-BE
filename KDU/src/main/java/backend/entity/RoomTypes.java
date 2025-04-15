package backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class RoomTypes {
    @Id
    private Long roomTypeId;

    @Column(name = "room_type_name", nullable = false, length = 1000)
    private String roomTypeName;

    @Column(name = "room_type_description", length = 2000)
    private String roomTypeDescription;

    @Column(name = "number_of_reviews")
    private int numberOfReviews;

    @Column(name = "stars")
    private double stars;

    @Column(name = "images", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode images;

    @Column(name = "amenities", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode amenities;

    @Column(name = "location", length = 1000)
    private String location;
}
