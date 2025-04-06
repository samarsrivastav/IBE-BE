package backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "booking_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingTransaction {
    @Id
    private Long id;
    
    @Column(name = "confirmation_id")
    private UUID confirmationId = UUID.randomUUID();
    
    @Column(name = "availability_id")
    @ElementCollection
    private List<Integer> availabilityId;
    
    @Column(name = "is_active")
    private boolean active;
    
    @Column(name = "booking_details", columnDefinition = "jsonb")
    private JsonNode bookingDetails;
    
    @Column(name = "email")
    private String email;

    @Column(name = "has_reviewed")
    private boolean hasReviewed;
} 