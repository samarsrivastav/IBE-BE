package backend.service;

import backend.entity.enums.CleaningStatus;
import backend.entity.enums.CleaningType;
import backend.entity.enums.RoomCleaningType;
import backend.entity.enums.Shift;
import backend.model.RoomCleaningSchedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface HouseCleaningService {
    /**
     * Generate cleaning schedules for a specific date
     * @param date The date for which to generate schedules
     * @return List of generated cleaning schedules
     */
    List<RoomCleaningSchedule> generateCleaningSchedules(LocalDate date);
    
    /**
     * Assign staff to cleaning schedules for a specific date and shift
     * @param date The date for which to assign staff
     * @param shift The shift for which to assign staff
     * @return List of updated cleaning schedules
     */
    List<RoomCleaningSchedule> assignStaffToSchedules(LocalDate date, Shift shift);
    
    /**
     * Update the status of a cleaning schedule
     * @param scheduleId The ID of the schedule to update
     * @param status The new status
     * @return The updated cleaning schedule
     */
    RoomCleaningSchedule updateCleaningStatus(Long scheduleId, CleaningStatus status);
    
    /**
     * Get all cleaning schedules for a specific date
     * @param date The date for which to get schedules
     * @return List of cleaning schedules
     */
    List<RoomCleaningSchedule> getSchedulesForDate(LocalDate date);
    
    /**
     * Get all cleaning schedules for a specific date and shift
     * @param date The date for which to get schedules
     * @param shift The shift for which to get schedules
     * @return List of cleaning schedules
     */
    List<RoomCleaningSchedule> getSchedulesForDateAndShift(LocalDate date, Shift shift);
    
    /**
     * Get all cleaning schedules for a specific date and room
     * @param date The date for which to get schedules
     * @param roomId The room ID for which to get schedules
     * @return List of cleaning schedules
     */
    List<RoomCleaningSchedule> getSchedulesForDateAndRoom(LocalDate date, String roomId);
    
    /**
     * Get all cleaning schedules for a specific date and room cleaning type
     * @param date The date for which to get schedules
     * @param roomCleaningType The room cleaning type for which to get schedules
     * @return List of cleaning schedules
     */
    List<RoomCleaningSchedule> getSchedulesForDateAndRoomType(LocalDate date, RoomCleaningType roomCleaningType);
    
    /**
     * Get the appropriate cleaning type for a room based on its cleaning type
     * @param roomCleaningType The room cleaning type
     * @return The appropriate cleaning type
     */
    CleaningType getCleaningTypeForRoomType(RoomCleaningType roomCleaningType);
    
    /**
     * Get the appropriate shift for a room based on its cleaning type and check-in/check-out times
     * @param roomCleaningType The room cleaning type
     * @param checkInTime The check-in time (if applicable)
     * @param checkOutTime The check-out time (if applicable)
     * @return The appropriate shift
     */
    Shift getShiftForRoomType(RoomCleaningType roomCleaningType, LocalTime checkInTime, LocalTime checkOutTime);
    
    /**
     * Get the appropriate scheduled time for a room based on its cleaning type and check-in/check-out times
     * @param roomCleaningType The room cleaning type
     * @param checkInTime The check-in time (if applicable)
     * @param checkOutTime The check-out time (if applicable)
     * @return The appropriate scheduled time
     */
    LocalTime getScheduledTimeForRoomType(RoomCleaningType roomCleaningType, LocalTime checkInTime, LocalTime checkOutTime);
} 