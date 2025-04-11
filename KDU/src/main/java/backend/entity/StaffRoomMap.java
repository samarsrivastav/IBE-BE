package backend.entity;

import backend.entity.enums.CleaningStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "staff_room_map")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffRoomMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_status", nullable = false)
    private CleaningStatus cleaningStatus = CleaningStatus.PENDING;
}
