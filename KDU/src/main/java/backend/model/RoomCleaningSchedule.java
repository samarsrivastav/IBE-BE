package backend.model;

import backend.entity.enums.CleaningStatus;
import backend.entity.enums.CleaningType;
import backend.entity.enums.RoomCleaningType;
import backend.entity.enums.Shift;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "room_cleaning_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCleaningSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_cleaning_type", nullable = false)
    private RoomCleaningType roomCleaningType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_type", nullable = false)
    private CleaningType cleaningType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_shift", nullable = false)
    private Shift assignedShift;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_status", nullable = false)
    private CleaningStatus cleaningStatus = CleaningStatus.PENDING;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "booking_id")
    private Long bookingId;
} 