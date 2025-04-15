package backend.service.impl;

import backend.entity.Staff;
import backend.entity.TenantConfiguration;
import backend.entity.enums.CleaningStatus;
import backend.entity.enums.CleaningType;
import backend.entity.enums.RoomCleaningType;
import backend.entity.enums.Shift;
import backend.model.BookingTransaction;
import backend.model.RoomCleaningSchedule;
import backend.repository.BookingTransactionRepository;
import backend.repository.RoomCleaningScheduleRepository;
import backend.repository.StaffRepository;
import backend.service.BookedRoomsService;
import backend.service.EmailService;
import backend.service.HouseCleaningService;
import backend.service.TenantConfigurationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseCleaningServiceImpl implements HouseCleaningService {

    private final BookingTransactionRepository bookingTransactionRepository;
    private final RoomCleaningScheduleRepository roomCleaningScheduleRepository;
    private final StaffRepository staffRepository;
    private final EmailService emailService;
    private final BookedRoomsService bookedRoomsService;
    private final TenantConfigurationService tenantConfigurationService;

    // Replace hardcoded constants with default values
    // These will be overridden by values from tenant configuration
    private static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(13, 0);
    private static final LocalTime DEFAULT_CHECK_OUT_TIME = LocalTime.of(11, 0);
    private static final int DEFAULT_DEEP_CLEANING_DURATION = 120; // 2 hours
    private static final int DEFAULT_DAILY_CLEANING_DURATION = 30; // 30 minutes
    private static final int DEFAULT_TIME_INTERVAL = 30; // 30 minutes
    private static final int DEFAULT_SHIFT_DURATION_HOURS = 4; // 4 hours

    // Default shift start times
    private static final LocalTime DEFAULT_MORNING_SHIFT_START = LocalTime.of(7, 0); // 7 AM
    private static final LocalTime DEFAULT_AFTERNOON_SHIFT_START = LocalTime.of(11, 0); // 11 AM
    private static final LocalTime DEFAULT_EVENING_SHIFT_START = LocalTime.of(15, 0); // 3 PM

    @Value("${property.id:8}")
    private int propertyId;

    @Value("${tenant.id:1}")
    private Long tenantId; // Default tenant ID, can be overridden in application properties

    @Override
    @Transactional
    public List<RoomCleaningSchedule> generateCleaningSchedules(LocalDate date) {
        log.info("Generating cleaning schedules for date: {}", date);

        // Get dynamic configuration values
        LocalTime checkInTime = getCheckInTime();
        LocalTime checkOutTime = getCheckOutTime();

        // Format date for repository queries
        String dateStr = date.format(DateTimeFormatter.ISO_DATE);

        // Get all bookings for the date
        List<BookingTransaction> checkInBookings = bookingTransactionRepository.findByCheckInDate(dateStr);
        List<BookingTransaction> checkOutBookings = bookingTransactionRepository.findByCheckOutDate(dateStr);
        List<BookingTransaction> currentBookings = bookingTransactionRepository.findCurrentBookings(dateStr);

        log.info("Found {} check-in bookings, {} check-out bookings, and {} current bookings for date {}",
                checkInBookings.size(), checkOutBookings.size(), currentBookings.size(), dateStr);

        // Get room IDs from BookedRoomsService
        List<Long> checkInRoomIds = bookedRoomsService.getCheckInRoomsForToday(propertyId);
        List<Long> checkOutRoomIds = bookedRoomsService.getCheckOutRoomsForToday(propertyId);
        List<Long> currentRoomIds = bookedRoomsService.getCurrentlyBookedRooms(propertyId);

        log.info("Found {} check-in rooms, {} check-out rooms, and {} current rooms for date {}",
                checkInRoomIds.size(), checkOutRoomIds.size(), currentRoomIds.size(), dateStr);

        List<RoomCleaningSchedule> schedules = new ArrayList<>();

        // Create a map of room IDs to booking IDs for quick lookup
        Map<Long, Long> roomToBookingMap = new HashMap<>();

        // Process check-in bookings
        for (BookingTransaction booking : checkInBookings) {
            JsonNode bookingDetails = booking.getBookingDetails();
            if (bookingDetails != null) {
                // Try to extract room ID from different possible locations in the booking details
                Long roomId = extractRoomIdFromBookingDetails(bookingDetails);
                if (roomId != null) {
                    roomToBookingMap.put(roomId, booking.getId());
                    log.info("Mapped room ID {} to booking ID {}", roomId, booking.getId());
                } else {
                    log.warn("Could not extract room ID from booking details for booking ID {}", booking.getId());
                }
            }
        }

        // Process check-out bookings
        for (BookingTransaction booking : checkOutBookings) {
            JsonNode bookingDetails = booking.getBookingDetails();
            if (bookingDetails != null) {
                // Try to extract room ID from different possible locations in the booking details
                Long roomId = extractRoomIdFromBookingDetails(bookingDetails);
                if (roomId != null) {
                    roomToBookingMap.put(roomId, booking.getId());
                    log.info("Mapped room ID {} to booking ID {}", roomId, booking.getId());
                } else {
                    log.warn("Could not extract room ID from booking details for booking ID {}", booking.getId());
                }
            }
        }

        // Process current bookings
        for (BookingTransaction booking : currentBookings) {
            JsonNode bookingDetails = booking.getBookingDetails();
            if (bookingDetails != null) {
                // Try to extract room ID from different possible locations in the booking details
                Long roomId = extractRoomIdFromBookingDetails(bookingDetails);
                if (roomId != null) {
                    roomToBookingMap.put(roomId, booking.getId());
                    log.info("Mapped room ID {} to booking ID {}", roomId, booking.getId());
                } else {
                    log.warn("Could not extract room ID from booking details for booking ID {}", booking.getId());
                }
            }
        }

        log.info("Room to booking map: {}", roomToBookingMap);

        // Process check-in and check-out bookings
        for (Long roomId : checkInRoomIds) {
            // Check if this room also has a check-out on the same day
            boolean hasCheckOut = checkOutRoomIds.contains(roomId);

            RoomCleaningType roomType = hasCheckOut ?
                    RoomCleaningType.CHECKIN_CHECKOUT : RoomCleaningType.CHECKIN_ONLY;

            CleaningType cleaningType = getCleaningTypeForRoomType(roomType);
            Shift shift = getShiftForRoomType(roomType, checkInTime, hasCheckOut ? checkOutTime : null);
            LocalTime scheduledTime = getScheduledTimeForRoomType(roomType, checkInTime, hasCheckOut ? checkOutTime : null);

            RoomCleaningSchedule schedule = new RoomCleaningSchedule();
            schedule.setRoomId(String.valueOf(roomId));
            schedule.setDate(date);
            schedule.setRoomCleaningType(roomType);
            schedule.setCleaningType(cleaningType);
            schedule.setAssignedShift(shift);
            schedule.setScheduledTime(scheduledTime);
            schedule.setCleaningStatus(CleaningStatus.PENDING);

            // Set booking ID from the map
            Long bookingId = roomToBookingMap.get(roomId);
            if (bookingId != null) {
                schedule.setBookingId(bookingId);
                log.info("Set booking ID {} for room ID {}", bookingId, roomId);
            } else {
                log.warn("No booking ID found for room ID {}", roomId);
            }

            schedules.add(schedule);
        }

        // Process check-out only bookings
        for (Long roomId : checkOutRoomIds) {
            if (!checkInRoomIds.contains(roomId) && !schedules.stream().anyMatch(s -> s.getRoomId().equals(String.valueOf(roomId)))) {
                RoomCleaningSchedule schedule = new RoomCleaningSchedule();
                schedule.setRoomId(String.valueOf(roomId));
                schedule.setDate(date);
                schedule.setRoomCleaningType(RoomCleaningType.CHECKOUT_ONLY);
                schedule.setCleaningType(CleaningType.DEEP_CLEANING);

                // Use the methods to determine shift and scheduled time with dynamic check-out time
                Shift shift = getShiftForRoomType(RoomCleaningType.CHECKOUT_ONLY, null, checkOutTime);
                LocalTime scheduledTime = getScheduledTimeForRoomType(RoomCleaningType.CHECKOUT_ONLY, null, checkOutTime);

                schedule.setAssignedShift(shift);
                schedule.setScheduledTime(scheduledTime);
                schedule.setCleaningStatus(CleaningStatus.PENDING);

                // Set booking ID from the map
                Long bookingId = roomToBookingMap.get(roomId);
                if (bookingId != null) {
                    schedule.setBookingId(bookingId);
                    log.info("Set booking ID {} for room ID {}", bookingId, roomId);
                } else {
                    log.warn("No booking ID found for room ID {}", roomId);
                }

                schedules.add(schedule);
            }
        }

        // Process booked rooms without check-in or check-out
        for (Long roomId : currentRoomIds) {
            if (!checkInRoomIds.contains(roomId) && !checkOutRoomIds.contains(roomId) &&
                    !schedules.stream().anyMatch(s -> s.getRoomId().equals(String.valueOf(roomId)))) {
                RoomCleaningSchedule schedule = new RoomCleaningSchedule();
                schedule.setRoomId(String.valueOf(roomId));
                schedule.setDate(date);
                schedule.setRoomCleaningType(RoomCleaningType.BOOKED_NO_CHANGE);
                schedule.setCleaningType(CleaningType.DAILY_CLEANING);

                // Use the methods to determine shift and scheduled time instead of hardcoding
                Shift shift = getShiftForRoomType(RoomCleaningType.BOOKED_NO_CHANGE, null, null);
                LocalTime scheduledTime = getScheduledTimeForRoomType(RoomCleaningType.BOOKED_NO_CHANGE, null, null);

                schedule.setAssignedShift(shift);
                schedule.setScheduledTime(scheduledTime);
                schedule.setCleaningStatus(CleaningStatus.PENDING);

                // Set booking ID from the map
                Long bookingId = roomToBookingMap.get(roomId);
                if (bookingId != null) {
                    schedule.setBookingId(bookingId);
                    log.info("Set booking ID {} for room ID {}", bookingId, roomId);
                } else {
                    log.warn("No booking ID found for room ID {}", roomId);
                }

                schedules.add(schedule);
            }
        }

        return roomCleaningScheduleRepository.saveAll(schedules);
    }

    /**
     * Extract room ID from booking details JSON
     * Tries different possible locations where the room ID might be stored
     */
    private Long extractRoomIdFromBookingDetails(JsonNode bookingDetails) {
        // Try direct roomId field
        if (bookingDetails.has("roomId")) {
            return bookingDetails.get("roomId").asLong();
        }

        // Try room.id field
        if (bookingDetails.has("room") && bookingDetails.get("room").has("id")) {
            return bookingDetails.get("room").get("id").asLong();
        }

        // Try rooms array
        if (bookingDetails.has("rooms") && bookingDetails.get("rooms").isArray() && bookingDetails.get("rooms").size() > 0) {
            JsonNode firstRoom = bookingDetails.get("rooms").get(0);
            if (firstRoom.has("id")) {
                return firstRoom.get("id").asLong();
            }
        }

        // Try confirmationDetails.roomId
        if (bookingDetails.has("confirmationDetails") && bookingDetails.get("confirmationDetails").has("roomId")) {
            return bookingDetails.get("confirmationDetails").get("roomId").asLong();
        }

        // Try availabilityId field (if it's a room ID)
        if (bookingDetails.has("availabilityId")) {
            return bookingDetails.get("availabilityId").asLong();
        }

        return null;
    }

    @Override
    @Transactional
    public List<RoomCleaningSchedule> assignStaffToSchedules(LocalDate date, Shift shift) {
        log.info("Assigning staff to schedules for date: {} and shift: {}", date, shift);

        // Get dynamic configuration values
        int shiftDurationHours = getShiftDuration();
        int dailyCleaningDuration = getDailyCleaningDuration();
        int deepCleaningDuration = getDeepCleaningDuration();

        // Get all pending schedules for the date and shift
        List<RoomCleaningSchedule> pendingSchedules = roomCleaningScheduleRepository
                .findByDateAndShiftAndStatus(date, shift, CleaningStatus.PENDING);

        if (pendingSchedules.isEmpty()) {
            log.info("No pending schedules found for date: {} and shift: {}", date, shift);
            return new ArrayList<>();
        }

        // Get all active staff for the shift
        List<Staff> activeStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
        
        // Get shift start and end times
        LocalTime shiftStartTime = getShiftStartTime(shift);
        LocalTime shiftEndTime = getShiftEndTime(shift);

        log.info("Shift time: {} to {}", shiftStartTime, shiftEndTime);

        // Sort schedules by cleaning type (deep cleaning first)
        pendingSchedules.sort((a, b) -> b.getCleaningType().compareTo(a.getCleaningType()));

        // Calculate total cleaning minutes required
        int totalMinutesRequired = 0;
        for (RoomCleaningSchedule schedule : pendingSchedules) {
            int cleaningDuration = schedule.getCleaningType() == CleaningType.DEEP_CLEANING ?
                    deepCleaningDuration : dailyCleaningDuration;
            totalMinutesRequired += cleaningDuration;
        }
        
        // Calculate minimum staff required based on total cleaning time
        int shiftMinutes = shiftDurationHours * 60;
        int minStaffRequired = (int) Math.ceil((double) totalMinutesRequired / shiftMinutes);
        
        log.info("STAFF ANALYSIS: Total cleaning minutes required: {}, Shift minutes: {}, Minimum staff required: {}, Staff available: {}",
                totalMinutesRequired, shiftMinutes, minStaffRequired, activeStaff.size());
        
        // Check if enough staff is available
        if (activeStaff.size() < minStaffRequired) {
            log.warn("STAFF SHORTAGE: Need {} staff but only {} available.", minStaffRequired, activeStaff.size());
            
            // Try to find staff that have not cleaned any rooms yet from other shifts
            List<Staff> availableStaffFromOtherShifts = findAvailableStaffFromOtherShifts(date, shift);
            log.info("Found {} available staff from other shifts", availableStaffFromOtherShifts.size());
            
            // If we still don't have enough staff after looking at other shifts, send an email
            if (activeStaff.size() + availableStaffFromOtherShifts.size() < minStaffRequired) {
                // Send a detailed email alert using the specialized method
                emailService.sendCriticalStaffShortageAlert(
                    date,
                    shift,
                    minStaffRequired,
                    pendingSchedules,
                    shiftDurationHours,
                    dailyCleaningDuration,
                    deepCleaningDuration,
                    shiftStartTime,
                    shiftEndTime
                );
                log.error("CRITICAL STAFF SHORTAGE: Email alert sent to admin.");
                
                // Calculate how many additional third-party staff are needed
                int additionalStaffNeeded = minStaffRequired - (activeStaff.size() + availableStaffFromOtherShifts.size());
                
                // Add third-party staff to meet the requirement
                List<Staff> thirdPartyStaff = addThirdPartyStaff(shift, additionalStaffNeeded);
                log.info("Added {} third-party staff to meet minimum requirement", thirdPartyStaff.size());
                
                // Add the third-party staff to active staff list
                activeStaff.addAll(thirdPartyStaff);
            }
            
            // Reassign staff from other shifts if available
            for (Staff staff : availableStaffFromOtherShifts) {
                String oldShift = staff.getShiftId();
                staff.setShiftId(shift.name());
                staffRepository.save(staff);
                activeStaff.add(staff);
                log.info("Reassigned staff {} from shift {} to shift {}", staff.getStaffId(), oldShift, shift);
                
                // Break if we've reached the minimum staff requirement
                if (activeStaff.size() >= minStaffRequired) {
                    break;
                }
            }
        }

        // Create maps to track staff assignment and utilization
        Map<Long, List<TimeSlot>> staffSchedules = new HashMap<>();
        Map<Long, Integer> staffMinutesBooked = new HashMap<>();

        // Initialize maps for all staff
        for (Staff staff : activeStaff) {
            staffSchedules.put(staff.getStaffId(), new ArrayList<>());
            staffMinutesBooked.put(staff.getStaffId(), 0);
        }

        // Assign schedules to staff using round-robin to balance workload
        int staffIndex = 0;
        int staffCount = activeStaff.size();
        
        // If no staff is available, we can't assign
        if (staffCount == 0) {
            log.error("CRITICAL ERROR: No staff available for shift {}. Schedules cannot be assigned.", shift);
            return pendingSchedules;
        }

        for (RoomCleaningSchedule schedule : pendingSchedules) {
            // Rotate through staff in round-robin fashion
            Long staffId = activeStaff.get(staffIndex % staffCount).getStaffId();
            staffIndex++;
            
            // Get current staff assignments
            List<TimeSlot> currentAssignments = staffSchedules.get(staffId);
            int currentMinutesBooked = staffMinutesBooked.get(staffId);
            
            // Calculate cleaning duration for this schedule
            int cleaningDuration = schedule.getCleaningType() == CleaningType.DEEP_CLEANING ?
                    deepCleaningDuration : dailyCleaningDuration;
            
            // Check if adding this assignment would exceed shift duration (100% utilization)
            if (currentMinutesBooked + cleaningDuration > shiftMinutes) {
                log.warn("Staff {} would exceed 100% utilization. Finding another staff member.", staffId);
                
                // Find another staff member who has capacity
                boolean foundStaff = false;
                for (int i = 0; i < staffCount; i++) {
                    Staff potentialStaff = activeStaff.get(i);
                    Long potentialStaffId = potentialStaff.getStaffId();
                    int potentialMinutesBooked = staffMinutesBooked.get(potentialStaffId);
                    
                    if (potentialMinutesBooked + cleaningDuration <= shiftMinutes) {
                        staffId = potentialStaffId;
                        currentAssignments = staffSchedules.get(staffId);
                        currentMinutesBooked = potentialMinutesBooked;
                        foundStaff = true;
                        break;
                    }
                }
                
                // If no staff has capacity, use the one with the least minutes booked
                if (!foundStaff) {
                    Long leastBusyStaffId = activeStaff.get(0).getStaffId();
                    int lowestMinutes = staffMinutesBooked.get(leastBusyStaffId);
                    
                    for (Staff staff : activeStaff) {
                        Long potentialStaffId = staff.getStaffId();
                        int minutes = staffMinutesBooked.get(potentialStaffId);
                        if (minutes < lowestMinutes) {
                            lowestMinutes = minutes;
                            leastBusyStaffId = potentialStaffId;
                        }
                    }
                    
                    staffId = leastBusyStaffId;
                    currentAssignments = staffSchedules.get(staffId);
                    currentMinutesBooked = lowestMinutes;
                    log.warn("No staff with full capacity available. Using least busy staff {}", staffId);
                }
            }
            
            // Find the next available time slot for this staff
            LocalTime startTime = shiftStartTime;
            if (!currentAssignments.isEmpty()) {
                // Sort assignments by end time to find the latest end time
                currentAssignments.sort(Comparator.comparing(ts -> ts.endTime));
                LocalTime latestEndTime = currentAssignments.get(currentAssignments.size() - 1).endTime;
                startTime = latestEndTime;
            }
            
            // Ensure the scheduled time respects the original scheduled time if possible
            LocalTime originalScheduledTime = schedule.getScheduledTime();
            if (originalScheduledTime != null && !originalScheduledTime.isBefore(shiftStartTime) && !originalScheduledTime.isAfter(shiftEndTime)) {
                // Check if the original scheduled time works
                boolean timeWorks = true;
                for (TimeSlot slot : currentAssignments) {
                    LocalTime potentialEndTime = originalScheduledTime.plusMinutes(cleaningDuration);
                    if (timeSlotsOverlap(originalScheduledTime, potentialEndTime, slot.startTime, slot.endTime)) {
                        timeWorks = false;
                        break;
                    }
                }
                
                if (timeWorks) {
                    startTime = originalScheduledTime;
                } else {
                    log.info("Cannot use original scheduled time {} for room {} - conflict with staff schedule", 
                            originalScheduledTime, schedule.getRoomId());
                }
            }
            
            // Calculate end time
            LocalTime endTime = startTime.plusMinutes(cleaningDuration);
            
            // Ensure we don't go beyond shift end time
            if (endTime.isAfter(shiftEndTime)) {
                // If the shift boundary would be exceeded, log a warning and adjust the time
                log.warn("Cleaning for room {} would end after shift ends. Adjusting start time.", schedule.getRoomId());
                
                // Try to schedule earlier in the shift if possible
                startTime = findEarliestAvailableTimeSlot(currentAssignments, shiftStartTime, shiftEndTime, cleaningDuration);
                endTime = startTime.plusMinutes(cleaningDuration);
                
                // If it still goes beyond shift end, it means the shift can't accommodate this cleaning
                if (endTime.isAfter(shiftEndTime)) {
                    log.error("CRITICAL: Room {} cannot be scheduled within shift boundaries.", schedule.getRoomId());
                    
                    // As a last resort, schedule at the start of the shift
                    startTime = shiftStartTime;
                    endTime = startTime.plusMinutes(cleaningDuration);
                }
            }
            
            // Assign the schedule to the staff
            schedule.setStaffId(staffId);
            schedule.setScheduledTime(startTime);
            
            // Update staff schedules
            staffSchedules.get(staffId).add(new TimeSlot(startTime, endTime));
            
            // Update staff minutes booked
            staffMinutesBooked.put(staffId, currentMinutesBooked + cleaningDuration);
            
            // Calculate utilization percentage
            double utilizationPercent = (currentMinutesBooked + cleaningDuration) * 100.0 / shiftMinutes;
            
            log.info("Assigned staff {} to room {} for cleaning from {} to {}, staff utilization={}%",
                    staffId, schedule.getRoomId(), startTime, endTime, utilizationPercent);
        }

        // Log the final assignments for debugging
        for (Staff staff : activeStaff) {
            Long staffId = staff.getStaffId();
            
            // Count how many rooms this staff is assigned to
            List<RoomCleaningSchedule> staffScheduleList = pendingSchedules.stream()
                    .filter(s -> s.getStaffId() != null && s.getStaffId().equals(staffId))
                    .sorted(Comparator.comparing(RoomCleaningSchedule::getScheduledTime))
                    .collect(Collectors.toList());
            
            int roomCount = staffScheduleList.size();
            
            if (roomCount == 0) {
                log.info("Staff {} is not assigned to any rooms in this shift.", staffId);
                continue;
            }
            
            // Calculate utilization percentage
            double utilizationPercent = staffMinutesBooked.get(staffId) * 100.0 / (shiftDurationHours * 60);
            
            log.info("Staff {} has {} rooms assigned (utilization: {}%)", staffId, roomCount, utilizationPercent);
            
            // Log each room assigned to this staff
            for (RoomCleaningSchedule schedule : staffScheduleList) {
                int cleaningDuration = schedule.getCleaningType() == CleaningType.DEEP_CLEANING ?
                        deepCleaningDuration : dailyCleaningDuration;
                LocalTime endTime = schedule.getScheduledTime().plusMinutes(cleaningDuration);
                log.info("  - Room {} from {} to {}", schedule.getRoomId(), schedule.getScheduledTime(), endTime);
            }
            
            // Calculate working hours
            if (!staffScheduleList.isEmpty()) {
                LocalTime earliestStart = staffScheduleList.get(0).getScheduledTime();
                LocalTime latestEnd = staffScheduleList.get(staffScheduleList.size() - 1).getScheduledTime();
                
                int lastCleaningDuration = staffScheduleList.get(staffScheduleList.size() - 1).getCleaningType() ==
                        CleaningType.DEEP_CLEANING ? deepCleaningDuration : dailyCleaningDuration;
                
                latestEnd = latestEnd.plusMinutes(lastCleaningDuration);
                
                log.info("  - Staff {} is working from {} to {} (utilization: {}%)",
                        staffId, earliestStart, latestEnd, utilizationPercent);
            }
        }

        // Save the schedules
        List<RoomCleaningSchedule> savedSchedules = roomCleaningScheduleRepository.saveAll(pendingSchedules);
        
        // Log the saved schedules to verify the times were preserved
        for (RoomCleaningSchedule schedule : savedSchedules) {
            log.info("Saved schedule for room {}: staffId={}, scheduledTime={}",
                    schedule.getRoomId(), schedule.getStaffId(), schedule.getScheduledTime());
        }
        
        return savedSchedules;
    }

    /**
     * Find the earliest available time slot for a staff that can accommodate the cleaning duration
     */
    private LocalTime findEarliestAvailableTimeSlot(List<TimeSlot> currentAssignments, LocalTime shiftStart, LocalTime shiftEnd, int cleaningDuration) {
        // If no assignments yet, return shift start
        if (currentAssignments.isEmpty()) {
            return shiftStart;
        }
        
        // Sort assignments by start time
        currentAssignments.sort(Comparator.comparing(ts -> ts.startTime));
        
        // Check if there's a gap at the beginning of the shift
        LocalTime firstStart = currentAssignments.get(0).startTime;
        if (firstStart.isAfter(shiftStart)) {
            int availableMinutes = (int) java.time.Duration.between(shiftStart, firstStart).toMinutes();
            if (availableMinutes >= cleaningDuration) {
                return shiftStart;
            }
        }
        
        // Check for gaps between assignments
        for (int i = 0; i < currentAssignments.size() - 1; i++) {
            LocalTime currentEnd = currentAssignments.get(i).endTime;
            LocalTime nextStart = currentAssignments.get(i + 1).startTime;
            
            int availableMinutes = (int) java.time.Duration.between(currentEnd, nextStart).toMinutes();
            if (availableMinutes >= cleaningDuration) {
                return currentEnd;
            }
        }
        
        // If no gaps found, use the end of the last assignment
        return currentAssignments.get(currentAssignments.size() - 1).endTime;
    }

    /**
     * Find available staff from other shifts who haven't yet been assigned any rooms
     */
    private List<Staff> findAvailableStaffFromOtherShifts(LocalDate date, Shift currentShift) {
        List<Staff> availableStaff = new ArrayList<>();
        
        for (Shift shift : Shift.values()) {
            if (shift == currentShift) {
                continue;
            }
            
            // Get staff from this shift
            List<Staff> shiftStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
            
            for (Staff staff : shiftStaff) {
                // Check if this staff has cleaned any rooms for any shift on this date
                List<RoomCleaningSchedule> staffSchedules = roomCleaningScheduleRepository
                        .findByDateAndStaffId(date, staff.getStaffId());
                
                // If they haven't cleaned any rooms, they can be reassigned
                if (staffSchedules.isEmpty()) {
                    availableStaff.add(staff);
                }
            }
        }
        
        return availableStaff;
    }

    /**
     * Check if two time slots overlap
     */
    private boolean timeSlotsOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    /**
     * Helper class to represent a time slot
     */
    private static class TimeSlot {
        LocalTime startTime;
        LocalTime endTime;

        TimeSlot(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    @Override
    @Transactional
    public RoomCleaningSchedule updateCleaningStatus(Long scheduleId, CleaningStatus status) {
        log.info("Updating cleaning status for schedule: {} to: {}", scheduleId, status);

        Optional<RoomCleaningSchedule> scheduleOpt = roomCleaningScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isPresent()) {
            RoomCleaningSchedule schedule = scheduleOpt.get();
            schedule.setCleaningStatus(status);
            return roomCleaningScheduleRepository.save(schedule);
        }

        return null;
    }

    @Override
    public List<RoomCleaningSchedule> getSchedulesForDate(LocalDate date) {
        return roomCleaningScheduleRepository.findByDate(date);
    }

    @Override
    public List<RoomCleaningSchedule> getSchedulesForDateAndShift(LocalDate date, Shift shift) {
        return roomCleaningScheduleRepository.findByDateAndAssignedShift(date, shift);
    }

    @Override
    public List<RoomCleaningSchedule> getSchedulesForDateAndRoom(LocalDate date, String roomId) {
        return roomCleaningScheduleRepository.findByDateAndRoomId(date, roomId);
    }

    @Override
    public List<RoomCleaningSchedule> getSchedulesForDateAndRoomType(LocalDate date, RoomCleaningType roomCleaningType) {
        return roomCleaningScheduleRepository.findByDateAndRoomCleaningType(date, roomCleaningType);
    }

    @Override
    public CleaningType getCleaningTypeForRoomType(RoomCleaningType roomCleaningType) {
        switch (roomCleaningType) {
            case CHECKIN_CHECKOUT:
                return CleaningType.DEEP_CLEANING;
            case CHECKIN_ONLY:
                return CleaningType.DAILY_CLEANING;
            case CHECKOUT_ONLY:
                return CleaningType.DEEP_CLEANING;
            case BOOKED_NO_CHANGE:
                return CleaningType.DAILY_CLEANING;
            default:
                return CleaningType.DAILY_CLEANING;
        }
    }

    @Override
    public Shift getShiftForRoomType(RoomCleaningType roomCleaningType, LocalTime checkInTime, LocalTime checkOutTime) {
        switch (roomCleaningType) {
            case CHECKIN_CHECKOUT:
                // For check-in and check-out on the same day, schedule between check-out and check-in
                // This should be after checkout time but before check-in time
                return Shift.AFTERNOON_SHIFT; // Checkout at 11 AM, so afternoon shift (11 AM - 3 PM) is appropriate
            case CHECKIN_ONLY:
                // For check-in only, schedule before check-in time
                return Shift.MORNING_SHIFT; // Morning shift (7 AM - 11 AM) is before 1 PM check-in
            case CHECKOUT_ONLY:
                // For check-out only, schedule after checkout time for deep cleaning
                return Shift.AFTERNOON_SHIFT; // After 11 AM checkout, so afternoon shift (11 AM - 3 PM)
            case BOOKED_NO_CHANGE:
                // For booked rooms without check-in or check-out, daily cleaning at any available time
                return Shift.AFTERNOON_SHIFT; // Keep as is, afternoon is fine for daily cleaning
            default:
                return Shift.AFTERNOON_SHIFT;
        }
    }

    @Override
    public LocalTime getScheduledTimeForRoomType(RoomCleaningType roomCleaningType, LocalTime checkInTime, LocalTime checkOutTime) {
        // Get dynamic configuration values
        int timeInterval = getTimeInterval();
        int shiftDurationHours = getShiftDuration();

        // Get the shift start time based on the room type
        Shift shift = getShiftForRoomType(roomCleaningType, checkInTime, checkOutTime);
        LocalTime shiftStartTime = getShiftStartTime(shift);

        // Calculate the total available time in the shift (in minutes)
        int totalShiftMinutes = shiftDurationHours * 60;

        // Calculate the number of intervals in the shift
        int totalIntervals = totalShiftMinutes / timeInterval;

        // Calculate a random interval within the shift (0 to totalIntervals-1)
        int intervalOffset = new Random().nextInt(totalIntervals);

        // Calculate the scheduled time by adding the interval offset to the shift start time
        return shiftStartTime.plusMinutes(intervalOffset * timeInterval);
    }


    /**
     * Get the start time for a shift
     */
    private LocalTime getShiftStartTime(Shift shift) {
        try {
            // Default values in case configuration is not found
            LocalTime morningShiftStart = DEFAULT_MORNING_SHIFT_START;
            LocalTime afternoonShiftStart = DEFAULT_AFTERNOON_SHIFT_START;
            LocalTime eveningShiftStart = DEFAULT_EVENING_SHIFT_START;

            // Try to get the configuration from tenant table
            List<TenantConfiguration> configurations = tenantConfigurationService.getConfigurationsByTenant(tenantId);

            if (!configurations.isEmpty()) {
                for (TenantConfiguration config : configurations) {
                    JsonNode configJson = config.getConfigurationJson();

                    // Check if shift timings are present in the configuration
                    if (configJson.has("shiftTimings")) {
                        JsonNode shiftTimings = configJson.get("shiftTimings");

                        if (shiftTimings.has("morningShift")) {
                            String morningTime = shiftTimings.get("morningShift").asText();
                            morningShiftStart = LocalTime.parse(morningTime);
                            log.info("Configured morning shift start time: {}", morningShiftStart);
                        }

                        if (shiftTimings.has("afternoonShift")) {
                            String afternoonTime = shiftTimings.get("afternoonShift").asText();
                            afternoonShiftStart = LocalTime.parse(afternoonTime);
                            log.info("Configured afternoon shift start time: {}", afternoonShiftStart);
                        }

                        if (shiftTimings.has("eveningShift")) {
                            String eveningTime = shiftTimings.get("eveningShift").asText();
                            eveningShiftStart = LocalTime.parse(eveningTime);
                            log.info("Configured evening shift start time: {}", eveningShiftStart);
                        }
                    }
                }
            } else {
                log.warn("No tenant configuration found, using default shift timings");
            }

            // Return the appropriate shift start time
            switch (shift) {
                case MORNING_SHIFT:
                    return morningShiftStart;
                case AFTERNOON_SHIFT:
                    return afternoonShiftStart;
                case EVENING_SHIFT:
                    return eveningShiftStart;
                default:
                    return morningShiftStart;
            }
        } catch (Exception e) {
            log.error("Error retrieving shift times from configuration: {}", e.getMessage(), e);
            // Fallback to default values
            switch (shift) {
                case MORNING_SHIFT:
                    return DEFAULT_MORNING_SHIFT_START;
                case AFTERNOON_SHIFT:
                    return DEFAULT_AFTERNOON_SHIFT_START;
                case EVENING_SHIFT:
                    return DEFAULT_EVENING_SHIFT_START;
                default:
                    return DEFAULT_MORNING_SHIFT_START;
            }
        }
    }

    /**
     * Calculates the end time for a shift
     * @param shift The shift to calculate end time for
     * @return The end time of the shift
     */
    private LocalTime getShiftEndTime(Shift shift) {
        LocalTime startTime = getShiftStartTime(shift);
        return startTime.plusHours(getShiftDuration());
    }



    // Helper method to get check-in time from configuration
    private LocalTime getCheckInTime() {
        try {
            // Try to get the configuration
            List<TenantConfiguration> configurations = tenantConfigurationService.getConfigurationsByTenant(tenantId);

            if (!configurations.isEmpty()) {
                for (TenantConfiguration config : configurations) {
                    JsonNode configJson = config.getConfigurationJson();

                    // Check if hotel timings are present in the configuration
                    if (configJson.has("hotelTimings") && configJson.get("hotelTimings").has("checkInTime")) {
                        String checkInTime = configJson.get("hotelTimings").get("checkInTime").asText();
                        LocalTime time = LocalTime.parse(checkInTime);
                        log.info("Using configured check-in time: {}", time);
                        return time;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving check-in time from configuration: {}", e.getMessage(), e);
        }

        log.info("Using default check-in time: {}", DEFAULT_CHECK_IN_TIME);
        return DEFAULT_CHECK_IN_TIME;
    }

    // Helper method to get check-out time from configuration
    private LocalTime getCheckOutTime() {
        try {
            // Try to get the configuration
            List<TenantConfiguration> configurations = tenantConfigurationService.getConfigurationsByTenant(tenantId);

            if (!configurations.isEmpty()) {
                for (TenantConfiguration config : configurations) {
                    JsonNode configJson = config.getConfigurationJson();

                    // Check if hotel timings are present in the configuration
                    if (configJson.has("hotelTimings") && configJson.get("hotelTimings").has("checkOutTime")) {
                        String checkOutTime = configJson.get("hotelTimings").get("checkOutTime").asText();
                        LocalTime time = LocalTime.parse(checkOutTime);
                        log.info("Using configured check-out time: {}", time);
                        return time;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving check-out time from configuration: {}", e.getMessage(), e);
        }

        log.info("Using default check-out time: {}", DEFAULT_CHECK_OUT_TIME);
        return DEFAULT_CHECK_OUT_TIME;
    }

    // Helper method to get deep cleaning duration from configuration
    private int getDeepCleaningDuration() {
        try {
            // Try to get the configuration
            List<TenantConfiguration> configurations = tenantConfigurationService.getConfigurationsByTenant(tenantId);

            if (!configurations.isEmpty()) {
                for (TenantConfiguration config : configurations) {
                    JsonNode configJson = config.getConfigurationJson();

                    // Check if cleaning durations are present in the configuration
                    if (configJson.has("cleaningDurations") && configJson.get("cleaningDurations").has("deepCleaning")) {
                        int duration = configJson.get("cleaningDurations").get("deepCleaning").asInt();
                        log.info("Using configured deep cleaning duration: {} minutes", duration);
                        return duration;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving deep cleaning duration from configuration: {}", e.getMessage(), e);
        }

        log.info("Using default deep cleaning duration: {} minutes", DEFAULT_DEEP_CLEANING_DURATION);
        return DEFAULT_DEEP_CLEANING_DURATION;
    }

    // Helper method to get daily cleaning duration from configuration
    private int getDailyCleaningDuration() {
        try {
            // Try to get the configuration
            List<TenantConfiguration> configurations = tenantConfigurationService.getConfigurationsByTenant(tenantId);

            if (!configurations.isEmpty()) {
                for (TenantConfiguration config : configurations) {
                    JsonNode configJson = config.getConfigurationJson();

                    // Check if cleaning durations are present in the configuration
                    if (configJson.has("cleaningDurations") && configJson.get("cleaningDurations").has("dailyCleaning")) {
                        int duration = configJson.get("cleaningDurations").get("dailyCleaning").asInt();
                        log.info("Using configured daily cleaning duration: {} minutes", duration);
                        return duration;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving daily cleaning duration from configuration: {}", e.getMessage(), e);
        }

        log.info("Using default daily cleaning duration: {} minutes", DEFAULT_DAILY_CLEANING_DURATION);
        return DEFAULT_DAILY_CLEANING_DURATION;
    }

    // Helper method to get time interval from configuration
    private int getTimeInterval() {
        try {
            // Try to get the configuration
            List<TenantConfiguration> configurations = tenantConfigurationService.getConfigurationsByTenant(tenantId);

            if (!configurations.isEmpty()) {
                for (TenantConfiguration config : configurations) {
                    JsonNode configJson = config.getConfigurationJson();

                    // Check if scheduling settings are present in the configuration
                    if (configJson.has("scheduling") && configJson.get("scheduling").has("timeInterval")) {
                        int interval = configJson.get("scheduling").get("timeInterval").asInt();
                        log.info("Using configured time interval: {} minutes", interval);
                        return interval;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving time interval from configuration: {}", e.getMessage(), e);
        }

        log.info("Using default time interval: {} minutes", DEFAULT_TIME_INTERVAL);
        return DEFAULT_TIME_INTERVAL;
    }

    // Helper method to get shift duration from configuration
    private int getShiftDuration() {
        try {
            // Try to get the configuration
            List<TenantConfiguration> configurations = tenantConfigurationService.getConfigurationsByTenant(tenantId);

            if (!configurations.isEmpty()) {
                for (TenantConfiguration config : configurations) {
                    JsonNode configJson = config.getConfigurationJson();

                    // Check if shift settings are present in the configuration
                    if (configJson.has("shifts") && configJson.get("shifts").has("duration")) {
                        int duration = configJson.get("shifts").get("duration").asInt();
                        log.info("Using configured shift duration: {} hours", duration);
                        return duration;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving shift duration from configuration: {}", e.getMessage(), e);
        }

        log.info("Using default shift duration: {} hours", DEFAULT_SHIFT_DURATION_HOURS);
        return DEFAULT_SHIFT_DURATION_HOURS;
    }

    // Add this new method to create third-party staff when there's a shortage
    /**
     * Add third-party staff to meet the minimum staff requirement
     * @param shift The shift that needs additional staff
     * @param countNeeded The number of third-party staff needed
     * @return List of newly created third-party staff
     */
    private List<Staff> addThirdPartyStaff(Shift shift, int countNeeded) {
        log.info("Adding {} third-party staff for shift {}", countNeeded, shift);
        
        List<Staff> newStaff = new ArrayList<>();
        
        for (int i = 0; i < countNeeded; i++) {
            Staff staff = new Staff();
            staff.setShiftId(shift.name());
            staff.setIsActive(true);
            staff.setIsThirdParty(true);
            
            Staff savedStaff = staffRepository.save(staff);
            newStaff.add(savedStaff);
            
            log.info("Added third-party staff with ID: {}", savedStaff.getStaffId());
        }
        
        return newStaff;
    }

    /**
     * Add third-party staff for a specific shift and reschedule cleaning tasks
     * This method is exposed as a public API to manually add third-party staff when needed
     */
    @Override
    @Transactional
    public List<Staff> addThirdPartyStaffAndReschedule(LocalDate date, Shift shift, int count) {
        log.info("Adding {} third-party staff for date: {} and shift: {}", count, date, shift);
        
        if (count <= 0) {
            log.warn("Invalid count of staff to add: {}", count);
            return new ArrayList<>();
        }
        
        // Add third-party staff
        List<Staff> addedStaff = addThirdPartyStaff(shift, count);
        
        // Get all pending schedules for the date and shift
        List<RoomCleaningSchedule> pendingSchedules = roomCleaningScheduleRepository
                .findByDateAndShiftAndStatus(date, shift, CleaningStatus.PENDING);
        
        if (!pendingSchedules.isEmpty()) {
            // If there are pending schedules, reassign them to include the new staff
            log.info("Rescheduling {} pending cleaning tasks to include new third-party staff", pendingSchedules.size());
            
            // Reset staff assignments to allow for reassignment
            for (RoomCleaningSchedule schedule : pendingSchedules) {
                if (schedule.getStaffId() != null) {
                    log.info("Resetting staff assignment for room: {}", schedule.getRoomId());
                    schedule.setStaffId(null);
                }
            }
            
            // Save schedules with reset staff assignments
            roomCleaningScheduleRepository.saveAll(pendingSchedules);
            
            // Reassign staff to schedules (this will now include the new third-party staff)
            assignStaffToSchedules(date, shift);
        } else {
            log.info("No pending schedules found for date: {} and shift: {}", date, shift);
        }
        
        return addedStaff;
    }

}