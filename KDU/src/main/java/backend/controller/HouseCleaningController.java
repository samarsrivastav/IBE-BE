package backend.controller;

import backend.entity.enums.CleaningStatus;
import backend.entity.enums.RoomCleaningType;
import backend.entity.enums.Shift;
import backend.model.RoomCleaningSchedule;
import backend.service.HouseCleaningService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/house-cleaning")
@RequiredArgsConstructor
public class HouseCleaningController {

    private final HouseCleaningService houseCleaningService;

    /**
     * Generate cleaning schedules for a specific date
     * @param date The date for which to generate schedules
     * @return List of generated cleaning schedules
     */
    @PostMapping("/generate")
    public ResponseEntity<List<RoomCleaningSchedule>> generateSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(houseCleaningService.generateCleaningSchedules(date));
    }

    /**
     * Assign staff to cleaning schedules for a specific date and shift
     * @param date The date for which to assign staff
     * @param shift The shift for which to assign staff
     * @return List of updated cleaning schedules
     */
    @PostMapping("/assign-staff")
    public ResponseEntity<List<RoomCleaningSchedule>> assignStaff(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Shift shift) {
        return ResponseEntity.ok(houseCleaningService.assignStaffToSchedules(date, shift));
    }

    /**
     * Update the status of a cleaning schedule
     * @param scheduleId The ID of the schedule to update
     * @param status The new status
     * @return The updated cleaning schedule
     */
    @PutMapping("/{scheduleId}/status")
    public ResponseEntity<RoomCleaningSchedule> updateStatus(
            @PathVariable Long scheduleId,
            @RequestParam CleaningStatus status) {
        return ResponseEntity.ok(houseCleaningService.updateCleaningStatus(scheduleId, status));
    }

    /**
     * Get all cleaning schedules for a specific date
     * @param date The date for which to get schedules
     * @return List of cleaning schedules
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<RoomCleaningSchedule>> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(houseCleaningService.getSchedulesForDate(date));
    }

    /**
     * Get all cleaning schedules for a specific date and shift
     * @param date The date for which to get schedules
     * @param shift The shift for which to get schedules
     * @return List of cleaning schedules
     */
    @GetMapping("/schedules/shift")
    public ResponseEntity<List<RoomCleaningSchedule>> getSchedulesByShift(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Shift shift) {
        return ResponseEntity.ok(houseCleaningService.getSchedulesForDateAndShift(date, shift));
    }

    /**
     * Get all cleaning schedules for a specific date and room
     * @param date The date for which to get schedules
     * @param roomId The room ID for which to get schedules
     * @return List of cleaning schedules
     */
    @GetMapping("/schedules/room")
    public ResponseEntity<List<RoomCleaningSchedule>> getSchedulesByRoom(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String roomId) {
        return ResponseEntity.ok(houseCleaningService.getSchedulesForDateAndRoom(date, roomId));
    }

    /**
     * Get all cleaning schedules for a specific date and room cleaning type
     * @param date The date for which to get schedules
     * @param roomCleaningType The room cleaning type for which to get schedules
     * @return List of cleaning schedules
     */
    @GetMapping("/schedules/room-type")
    public ResponseEntity<List<RoomCleaningSchedule>> getSchedulesByRoomType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam RoomCleaningType roomCleaningType) {
        return ResponseEntity.ok(houseCleaningService.getSchedulesForDateAndRoomType(date, roomCleaningType));
    }
} 