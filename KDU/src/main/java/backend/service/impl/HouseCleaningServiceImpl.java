package backend.service.impl;

import backend.entity.Staff;
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
import com.fasterxml.jackson.databind.JsonNode;
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

    // Check-in time is 11 AM
    private static final LocalTime CHECK_IN_TIME = LocalTime.of(13, 0);
    // Check-out time is 9 AM
    private static final LocalTime CHECK_OUT_TIME = LocalTime.of(11, 0);
    // Cleaning durations in minutes
    private static final int DEEP_CLEANING_DURATION = 120; // 2 hours
    private static final int DAILY_CLEANING_DURATION = 30; // 30 minutes
    // Time interval for scheduling (30 minutes)
    private static final int TIME_INTERVAL = 30;
    // Shift duration in hours
    private static final int SHIFT_DURATION_HOURS = 4;
    
    @Value("${property.id:8}")
    private int propertyId;

    @Override
    @Transactional
    public List<RoomCleaningSchedule> generateCleaningSchedules(LocalDate date) {
        log.info("Generating cleaning schedules for date: {}", date);
        
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
            Shift shift = getShiftForRoomType(roomType, CHECK_IN_TIME, hasCheckOut ? CHECK_OUT_TIME : null);
            LocalTime scheduledTime = getScheduledTimeForRoomType(roomType, CHECK_IN_TIME, hasCheckOut ? CHECK_OUT_TIME : null);
            
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
                schedule.setAssignedShift(Shift.MORNING_SHIFT);
                schedule.setScheduledTime(LocalTime.of(6, 0)); // 6 AM
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
                schedule.setAssignedShift(Shift.AFTERNOON_SHIFT);
                schedule.setScheduledTime(LocalTime.of(10, 0)); // 10 AM
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
        
        // Get all pending schedules for the date and shift
        List<RoomCleaningSchedule> pendingSchedules = roomCleaningScheduleRepository
                .findByDateAndShiftAndStatus(date, shift, CleaningStatus.PENDING);
        
        if (pendingSchedules.isEmpty()) {
            log.info("No pending schedules found for date: {} and shift: {}", date, shift);
            return new ArrayList<>();
        }
        
        // Get all active staff for the shift
        List<Staff> activeStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
        
        if (activeStaff.isEmpty()) {
            log.warn("No active staff found for shift: {}", shift);
            return pendingSchedules;
        }
        
        // Get shift start and end times
        LocalTime shiftStartTime = getShiftStartTime(shift);
        LocalTime shiftEndTime = shiftStartTime.plusHours(SHIFT_DURATION_HOURS);
        
        log.info("Shift time: {} to {}", shiftStartTime, shiftEndTime);
        
        // Calculate staff capacity
        int roomsPerStaff = SHIFT_DURATION_HOURS * 60 / DAILY_CLEANING_DURATION; // 4 hours * 60 minutes / 30 minutes = 8 rooms
        int totalStaffCapacity = activeStaff.size() * roomsPerStaff;
        
        // Check if we have enough staff capacity
        if (pendingSchedules.size() > totalStaffCapacity) {
            log.warn("Not enough staff capacity for all rooms. Need {} more staff members.", 
                    (pendingSchedules.size() - totalStaffCapacity + roomsPerStaff - 1) / roomsPerStaff);
            // In a real system, we would send an email alert here
        }
        
        // Sort schedules by cleaning type (deep cleaning first)
        pendingSchedules.sort((a, b) -> b.getCleaningType().compareTo(a.getCleaningType()));
        
        // Create maps to track staff assignment and utilization
        Map<Long, List<TimeSlot>> staffSchedules = new HashMap<>();
        Map<Long, Integer> staffMinutesBooked = new HashMap<>();
        
        // Initialize maps for all staff
        for (Staff staff : activeStaff) {
            staffSchedules.put(staff.getStaffId(), new ArrayList<>());
            staffMinutesBooked.put(staff.getStaffId(), 0);
        }
        
        // Determine how many staff we actually need
        int requiredStaffCount = Math.min(activeStaff.size(), 
                                         (pendingSchedules.size() + roomsPerStaff - 1) / roomsPerStaff);
        
        // Create a list to keep track of unassigned schedules
        List<RoomCleaningSchedule> unassignedSchedules = new ArrayList<>();
        
        // Create a list of staff IDs we'll use
        List<Long> staffIds = new ArrayList<>();
        for (int i = 0; i < requiredStaffCount; i++) {
            staffIds.add(activeStaff.get(i).getStaffId());
        }
        
        // Assign schedules to staff
        Map<Long, LocalTime> nextAvailableTime = new HashMap<>();
        for (Long staffId : staffIds) {
            nextAvailableTime.put(staffId, shiftStartTime);
        }
        
        // Process deep cleaning schedules first
        for (RoomCleaningSchedule schedule : pendingSchedules) {
            // Find the least utilized staff (based on next available time)
            Long leastUtilizedStaffId = staffIds.get(0);
            LocalTime earliestTime = nextAvailableTime.get(leastUtilizedStaffId);
            
            for (Long staffId : staffIds) {
                LocalTime staffTime = nextAvailableTime.get(staffId);
                if (staffTime.isBefore(earliestTime)) {
                    earliestTime = staffTime;
                    leastUtilizedStaffId = staffId;
                }
            }
            
            // Calculate cleaning duration for this schedule
            int cleaningDuration = schedule.getCleaningType() == CleaningType.DEEP_CLEANING ? 
                    DEEP_CLEANING_DURATION : DAILY_CLEANING_DURATION;
            
            // Get the current staff's next available time
            LocalTime startTime = nextAvailableTime.get(leastUtilizedStaffId);
            LocalTime endTime = startTime.plusMinutes(cleaningDuration);
            
            // Ensure we don't go beyond shift end time
            if (endTime.isAfter(shiftEndTime)) {
                // This staff can't take any more assignments
                staffIds.remove(leastUtilizedStaffId);
                
                // If no more staff is available, add to unassigned
                if (staffIds.isEmpty()) {
                    unassignedSchedules.add(schedule);
                    continue;
                }
                
                // Try with the next staff
                leastUtilizedStaffId = staffIds.get(0);
                startTime = nextAvailableTime.get(leastUtilizedStaffId);
                endTime = startTime.plusMinutes(cleaningDuration);
                
                // If still beyond shift end, add to unassigned
                if (endTime.isAfter(shiftEndTime)) {
                    unassignedSchedules.add(schedule);
                    continue;
                }
            }
            
            // Assign the schedule to the staff
            schedule.setStaffId(leastUtilizedStaffId);
            schedule.setScheduledTime(startTime);
            
            // Update staff schedules
            staffSchedules.get(leastUtilizedStaffId).add(new TimeSlot(startTime, endTime));
            
            // Update staff minutes booked
            int currentMinutes = staffMinutesBooked.get(leastUtilizedStaffId);
            staffMinutesBooked.put(leastUtilizedStaffId, currentMinutes + cleaningDuration);
            
            // Update next available time for this staff
            nextAvailableTime.put(leastUtilizedStaffId, endTime);
            
            log.info("Assigned staff {} to room {} for cleaning from {} to {}, staff utilization={}%", 
                    leastUtilizedStaffId, schedule.getRoomId(), startTime, endTime,
                    (staffMinutesBooked.get(leastUtilizedStaffId) * 100.0 / (SHIFT_DURATION_HOURS * 60)));
        }
        
        // Handle any unassigned schedules as a last resort
        for (RoomCleaningSchedule schedule : unassignedSchedules) {
            // Find the staff with lowest total minutes booked
            Long leastUtilizedStaffId = activeStaff.get(0).getStaffId();
            int lowestMinutes = staffMinutesBooked.get(leastUtilizedStaffId);
            
            for (Staff staff : activeStaff) {
                int minutes = staffMinutesBooked.get(staff.getStaffId());
                if (minutes < lowestMinutes) {
                    lowestMinutes = minutes;
                    leastUtilizedStaffId = staff.getStaffId();
                }
            }
            
            // Calculate cleaning duration for this schedule
            int cleaningDuration = schedule.getCleaningType() == CleaningType.DEEP_CLEANING ? 
                    DEEP_CLEANING_DURATION : DAILY_CLEANING_DURATION;
            
            // Assign to the least utilized staff at the shift start time
            schedule.setStaffId(leastUtilizedStaffId);
            schedule.setScheduledTime(shiftStartTime);
            
            // Update staff utilization
            staffMinutesBooked.put(leastUtilizedStaffId, lowestMinutes + cleaningDuration);
            
            log.warn("Last resort assignment: Staff {} to room {} at shift start time", 
                    leastUtilizedStaffId, schedule.getRoomId());
        }
        
        // Log the final assignments for debugging
        for (Staff staff : activeStaff) {
            Long staffId = staff.getStaffId();
            
            // Count how many rooms this staff is assigned to
            int roomCount = 0;
            for (RoomCleaningSchedule schedule : pendingSchedules) {
                if (schedule.getStaffId() != null && schedule.getStaffId().equals(staffId)) {
                    roomCount++;
                }
            }
            
            if (roomCount == 0) {
                log.info("Staff {} is not assigned to any rooms in this shift. Consider moving to another shift.", staffId);
                continue;
            }
            
            log.info("Staff {} has {} rooms assigned (utilization: {}%)", staffId, roomCount, 
                    (staffMinutesBooked.get(staffId) * 100.0 / (SHIFT_DURATION_HOURS * 60)));
            
            // Log each room assigned to this staff
            List<RoomCleaningSchedule> staffSchedulesSorted = pendingSchedules.stream()
                .filter(s -> s.getStaffId() != null && s.getStaffId().equals(staffId))
                .sorted(Comparator.comparing(RoomCleaningSchedule::getScheduledTime))
                .collect(Collectors.toList());
            
            for (RoomCleaningSchedule schedule : staffSchedulesSorted) {
                log.info("  - Room {} at {}", schedule.getRoomId(), schedule.getScheduledTime());
            }
            
            // Calculate working hours
            if (!staffSchedulesSorted.isEmpty()) {
                LocalTime earliestStart = staffSchedulesSorted.get(0).getScheduledTime();
                LocalTime latestEnd = staffSchedulesSorted.get(staffSchedulesSorted.size() - 1).getScheduledTime();
                
                int lastCleaningDuration = staffSchedulesSorted.get(staffSchedulesSorted.size() - 1).getCleaningType() == 
                    CleaningType.DEEP_CLEANING ? DEEP_CLEANING_DURATION : DAILY_CLEANING_DURATION;
                
                latestEnd = latestEnd.plusMinutes(lastCleaningDuration);
                
                log.info("  - Staff {} is working from {} to {} (utilization: {}%)", 
                        staffId, earliestStart, latestEnd, 
                        (staffMinutesBooked.get(staffId) * 100.0 / (SHIFT_DURATION_HOURS * 60)));
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
     * Get the start time for a shift
     */
    private LocalTime getShiftStartTime(Shift shift) {
        switch (shift) {
            case MORNING_SHIFT:
                return LocalTime.of(7, 0); // 7 AM
            case AFTERNOON_SHIFT:
                return LocalTime.of(11, 0); // 11 AM
            case EVENING_SHIFT:
                return LocalTime.of(15, 0); // 3 PM
            default:
                return LocalTime.of(7, 0); // Default to 7 AM
        }
    }
    
    /**
     * Find an available staff member for the given time slot
     */
    private Staff findAvailableStaff(List<Staff> activeStaff, Map<Long, List<TimeSlot>> staffAvailability, 
                                     LocalTime startTime, LocalTime endTime) {
        // Try to find a staff member who is not busy during this time slot
        for (Staff staff : activeStaff) {
            List<TimeSlot> staffSlots = staffAvailability.get(staff.getStaffId());
            
            // Check if the staff is available during the requested time slot
            boolean isAvailable = true;
            for (TimeSlot slot : staffSlots) {
                if (timeSlotsOverlap(startTime, endTime, slot.startTime, slot.endTime)) {
                    isAvailable = false;
                    break;
                }
            }
            
            if (isAvailable) {
                return staff;
            }
        }
        
        // If no staff is available for the exact time slot, try to find one who can be assigned
        // by adjusting the schedule slightly
        for (Staff staff : activeStaff) {
            List<TimeSlot> staffSlots = staffAvailability.get(staff.getStaffId());
            
            // Sort the staff's existing slots by start time
            staffSlots.sort(Comparator.comparing(slot -> slot.startTime));
            
            // Find gaps between existing slots
            for (int i = 0; i < staffSlots.size() - 1; i++) {
                TimeSlot currentSlot = staffSlots.get(i);
                TimeSlot nextSlot = staffSlots.get(i + 1);
                
                // Calculate the gap between slots
                LocalTime gapStart = currentSlot.endTime;
                LocalTime gapEnd = nextSlot.startTime;
                
                // Check if the requested time slot can fit in this gap
                if (gapEnd.isAfter(gapStart) && 
                    !startTime.isBefore(gapStart) && 
                    !endTime.isAfter(gapEnd)) {
                    return staff;
                }
            }
            
            // Check if the slot can be added before the first slot
            if (!staffSlots.isEmpty()) {
                TimeSlot firstSlot = staffSlots.get(0);
                if (!startTime.isBefore(firstSlot.startTime) && 
                    !endTime.isAfter(firstSlot.startTime)) {
                    return staff;
                }
            }
            
            // Check if the slot can be added after the last slot
            if (!staffSlots.isEmpty()) {
                TimeSlot lastSlot = staffSlots.get(staffSlots.size() - 1);
                if (!startTime.isBefore(lastSlot.endTime) && 
                    !endTime.isAfter(lastSlot.endTime.plusHours(1))) { // Allow some flexibility
                    return staff;
                }
            }
        }
        
        return null;
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
                return Shift.MORNING_SHIFT;
            case CHECKIN_ONLY:
                // For check-in only, schedule before check-in time
                return Shift.MORNING_SHIFT;
            case CHECKOUT_ONLY:
                // For check-out only, schedule early morning
                return Shift.MORNING_SHIFT;
            case BOOKED_NO_CHANGE:
                // For booked rooms without check-in or check-out, schedule in the afternoon
                return Shift.AFTERNOON_SHIFT;
            default:
                return Shift.AFTERNOON_SHIFT;
        }
    }

    @Override
    public LocalTime getScheduledTimeForRoomType(RoomCleaningType roomCleaningType, LocalTime checkInTime, LocalTime checkOutTime) {
        // Get the shift start time based on the room type
        Shift shift = getShiftForRoomType(roomCleaningType, checkInTime, checkOutTime);
        LocalTime shiftStartTime = getShiftStartTime(shift);
        
        // Calculate the total available time in the shift (in minutes)
        int totalShiftMinutes = SHIFT_DURATION_HOURS * 60;
        
        // Calculate the number of intervals in the shift
        int totalIntervals = totalShiftMinutes / TIME_INTERVAL;
        
        // Calculate a random interval within the shift (0 to totalIntervals-1)
        int intervalOffset = new Random().nextInt(totalIntervals);
        
        // Calculate the scheduled time by adding the interval offset to the shift start time
        return shiftStartTime.plusMinutes(intervalOffset * TIME_INTERVAL);
    }

    /**
     * Calculate the utilization percentage of a staff member's shift
     */
    private double calculateUtilizationPercentage(List<TimeSlot> slots, LocalTime shiftStart, LocalTime shiftEnd) {
        // Calculate total shift minutes
        long totalShiftMinutes = SHIFT_DURATION_HOURS * 60;
        
        // Calculate total assigned minutes (accounting for overlaps)
        Set<Integer> minutesWorked = new HashSet<>();
        
        for (TimeSlot slot : slots) {
            LocalTime start = slot.startTime;
            LocalTime end = slot.endTime;
            
            // Ensure we're within shift boundaries
            if (start.isBefore(shiftStart)) {
                start = shiftStart;
            }
            if (end.isAfter(shiftEnd)) {
                end = shiftEnd;
            }
            
            // Add each minute to the set
            LocalTime current = start;
            while (!current.isAfter(end.minusMinutes(1))) {
                int minuteOfDay = current.getHour() * 60 + current.getMinute();
                minutesWorked.add(minuteOfDay);
                current = current.plusMinutes(1);
            }
        }
        
        // Calculate percentage
        return (double) minutesWorked.size() / totalShiftMinutes * 100;
    }
} 