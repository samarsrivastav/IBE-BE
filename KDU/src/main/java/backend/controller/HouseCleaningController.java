package backend.controller;

import backend.entity.Staff;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/house-cleaning")
@RequiredArgsConstructor
public class HouseCleaningController {

    private final HouseCleaningService houseCleaningService;

    /**
     * Generate cleaning schedules for a specific date
     * @param date The date for which to generate schedules
     * @return List of generated cleaning schedules
     */
    @PostMapping("/generate-schedules")
    public ResponseEntity<List<RoomCleaningSchedule>> generateSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<RoomCleaningSchedule> schedules = houseCleaningService.generateCleaningSchedules(date);
        return ResponseEntity.ok(schedules);
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
        List<RoomCleaningSchedule> schedules = houseCleaningService.assignStaffToSchedules(date, shift);
        return ResponseEntity.ok(schedules);
    }

    /**
     * Update the status of a cleaning schedule
     * @param scheduleId The ID of the schedule to update
     * @param status The new status
     * @return The updated cleaning schedule
     */
    @PutMapping("/update-status/{scheduleId}")
    public ResponseEntity<RoomCleaningSchedule> updateStatus(
            @PathVariable Long scheduleId,
            @RequestParam CleaningStatus status) {
        RoomCleaningSchedule schedule = houseCleaningService.updateCleaningStatus(scheduleId, status);
        if (schedule != null) {
            return ResponseEntity.ok(schedule);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get all cleaning schedules for a specific date
     * @param date The date for which to get schedules
     * @return List of cleaning schedules
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<RoomCleaningSchedule>> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<RoomCleaningSchedule> schedules = houseCleaningService.getSchedulesForDate(date);
        return ResponseEntity.ok(schedules);
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
        List<RoomCleaningSchedule> schedules = houseCleaningService.getSchedulesForDateAndShift(date, shift);
        return ResponseEntity.ok(schedules);
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
        List<RoomCleaningSchedule> schedules = houseCleaningService.getSchedulesForDateAndRoom(date, roomId);
        return ResponseEntity.ok(schedules);
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
        List<RoomCleaningSchedule> schedules = houseCleaningService.getSchedulesForDateAndRoomType(date, roomCleaningType);
        return ResponseEntity.ok(schedules);
    }
    
    @PostMapping("/add-third-party-staff")
    public ResponseEntity<Map<String, Object>> addThirdPartyStaff(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Shift shift,
            @RequestParam int count) {
        List<Staff> addedStaff = houseCleaningService.addThirdPartyStaffAndReschedule(date, shift, count);
        
        Map<String, Object> response = Map.of(
            "success", !addedStaff.isEmpty(),
            "message", String.format("Added %d third-party staff for shift %s", addedStaff.size(), shift),
            "staffCount", addedStaff.size(),
            "staffIds", addedStaff.stream().map(Staff::getStaffId).toList()
        );
        
        return ResponseEntity.ok(response);
    }
} 