package backend.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pseudo_booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PseudoBooking {
    @EmbeddedId
    private PseudoBookingId pseudoBookingId;

    private long bookingGroupId;  // To track which entries belong to the same booking attempt

    @Version
    private int version;
} 