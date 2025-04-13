package backend.repository;

import backend.entity.enums.CleaningStatus;
import backend.entity.enums.RoomCleaningType;
import backend.entity.enums.Shift;
import backend.model.RoomCleaningSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface RoomCleaningScheduleRepository extends JpaRepository<RoomCleaningSchedule, Long> {
    List<RoomCleaningSchedule> findByDate(LocalDate date);
    List<RoomCleaningSchedule> findByDateAndRoomId(LocalDate date, String roomId);
    List<RoomCleaningSchedule> findByDateAndRoomCleaningType(LocalDate date, RoomCleaningType roomCleaningType);
    List<RoomCleaningSchedule> findByDateAndAssignedShift(LocalDate date, Shift shift);
    List<RoomCleaningSchedule> findByDateAndCleaningStatus(LocalDate date, CleaningStatus status);
    List<RoomCleaningSchedule> findByDateAndStaffId(LocalDate date, Long staffId);
    
    @Query("SELECT r FROM RoomCleaningSchedule r WHERE r.date = :date AND r.assignedShift = :shift AND r.cleaningStatus = :status")
    List<RoomCleaningSchedule> findByDateAndShiftAndStatus(
            @Param("date") LocalDate date, 
            @Param("shift") Shift shift, 
            @Param("status") CleaningStatus status);
    
    @Query("SELECT r FROM RoomCleaningSchedule r WHERE r.date = :date AND r.scheduledTime BETWEEN :startTime AND :endTime")
    List<RoomCleaningSchedule> findByDateAndTimeRange(
            @Param("date") LocalDate date, 
            @Param("startTime") LocalTime startTime, 
            @Param("endTime") LocalTime endTime);
} 