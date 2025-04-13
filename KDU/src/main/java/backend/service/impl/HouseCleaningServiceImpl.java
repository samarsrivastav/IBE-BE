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
        
        // Calculate how many staff we actually need for this shift
        int roomsPerStaff = SHIFT_DURATION_HOURS * 60 / DAILY_CLEANING_DURATION; 
        int requiredStaffCount = (int) Math.ceil((double) pendingSchedules.size() / roomsPerStaff);
        
        // Get all active staff for the shift
        List<Staff> activeStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
        
        // Check if we need to redistribute staff before proceeding
        if (activeStaff.isEmpty() || activeStaff.size() < requiredStaffCount) {
            log.warn("Staff shortage detected for shift {}. Required: {}, Available: {}. Attempting to redistribute staff from other shifts.", 
                    shift, requiredStaffCount, activeStaff.size());
            
            // Redistribute staff from other shifts to this one based on need
            redistributeStaffAcrossShifts(date, shift, requiredStaffCount);
            
            // Refresh the active staff list after redistribution
            activeStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
            
            if (activeStaff.isEmpty()) {
                log.error("CRITICAL STAFF SHORTAGE: No staff available for shift {} after redistribution attempt.", shift);
                return pendingSchedules;
            } else {
                log.info("After redistribution: {} staff available for shift {}", activeStaff.size(), shift);
            }
        }
        
        // Get shift start and end times
        LocalTime shiftStartTime = getShiftStartTime(shift);
        LocalTime shiftEndTime = shiftStartTime.plusHours(SHIFT_DURATION_HOURS);
        
        log.info("Shift time: {} to {}", shiftStartTime, shiftEndTime);
        
        // Calculate staff capacity
        int totalStaffCapacity = activeStaff.size() * roomsPerStaff;
        
        // Log staff requirements
        if (requiredStaffCount <= activeStaff.size()) {
            log.info("STAFF REQUIREMENTS: {} staff needed for {} rooms in shift {}. {} staff available, {} excess staff.", 
                    requiredStaffCount, pendingSchedules.size(), shift, activeStaff.size(), activeStaff.size() - requiredStaffCount);
        } else {
            log.warn("STAFF SHORTAGE: {} staff needed for {} rooms in shift {}. Only {} staff available. Need {} more staff.", 
                    requiredStaffCount, pendingSchedules.size(), shift, activeStaff.size(), requiredStaffCount - activeStaff.size());
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
        int staffToUseCount = Math.min(activeStaff.size(), requiredStaffCount);
        
        // Create a list to keep track of unassigned schedules
        List<RoomCleaningSchedule> unassignedSchedules = new ArrayList<>();
        
        // Create a list of staff IDs we'll use
        List<Long> staffIds = new ArrayList<>();
        for (int i = 0; i < staffToUseCount; i++) {
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
        
        // Keep track of staff usage status
        Set<Long> usedStaffIds = new HashSet<>();
        Set<Long> underutilizedStaffIds = new HashSet<>();
        Set<Long> unusedStaffIds = new HashSet<>();
        
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
                log.info("Staff {} is not assigned to any rooms in this shift. Will move to another shift.", staffId);
                unusedStaffIds.add(staffId);
                continue;
            }
            
            // Calculate utilization percentage
            double utilizationPercent = staffMinutesBooked.get(staffId) * 100.0 / (SHIFT_DURATION_HOURS * 60);
            
            if (utilizationPercent < 70) {
                underutilizedStaffIds.add(staffId);
                log.info("Staff {} is underutilized ({}%). Consider moving to another shift.", 
                        staffId, utilizationPercent);
            } else {
                usedStaffIds.add(staffId);
            }
            
            log.info("Staff {} has {} rooms assigned (utilization: {}%)", staffId, roomCount, utilizationPercent);
            
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
                        staffId, earliestStart, latestEnd, utilizationPercent);
            }
        }
        
        // Reassign unused staff to other shifts where needed
        if (!unusedStaffIds.isEmpty() || !underutilizedStaffIds.isEmpty()) {
            reassignStaffToOtherShifts(date, shift, unusedStaffIds, underutilizedStaffIds);
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
    
    /**
     * Reassign staff to other shifts where they might be needed
     */
    private void reassignStaffToOtherShifts(LocalDate date, Shift currentShift, 
                                           Set<Long> unusedStaffIds, Set<Long> underutilizedStaffIds) {
        // Get counts for all shifts to determine where staff is needed
        Map<Shift, Integer> shiftRoomCounts = new HashMap<>();
        Map<Shift, Integer> shiftStaffCounts = new HashMap<>();
        
        for (Shift shift : Shift.values()) {
            // Skip the current shift
            if (shift == currentShift) {
                continue;
            }
            
            // Get room count for this shift
            List<RoomCleaningSchedule> shiftSchedules = roomCleaningScheduleRepository
                .findByDateAndShiftAndStatus(date, shift, CleaningStatus.PENDING);
            shiftRoomCounts.put(shift, shiftSchedules.size());
            
            // Get staff count for this shift
            List<Staff> shiftStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
            shiftStaffCounts.put(shift, shiftStaff.size());
            
            // Calculate required staff for this shift
            int roomsPerStaff = SHIFT_DURATION_HOURS * 60 / DAILY_CLEANING_DURATION;
            int requiredStaff = (int) Math.ceil((double) shiftSchedules.size() / roomsPerStaff);
            
            log.info("STAFF ANALYSIS - Shift {}: {} rooms, {} staff available, {} staff required", 
                    shift, shiftSchedules.size(), shiftStaff.size(), requiredStaff);
        }
        
        // First, handle unused staff
        if (!unusedStaffIds.isEmpty()) {
            log.info("Attempting to reassign {} unused staff members from {} shift", 
                    unusedStaffIds.size(), currentShift);
            
            for (Long staffId : unusedStaffIds) {
                Shift targetShift = findMostNeededShift(shiftRoomCounts, shiftStaffCounts);
                
                if (targetShift != null) {
                    // Move this staff to the target shift
                    Optional<Staff> staffOpt = staffRepository.findById(staffId.toString());
                    if (staffOpt.isPresent()) {
                        Staff staff = staffOpt.get();
                        String oldShift = staff.getShiftId();
                        staff.setShiftId(targetShift.name());
                        staffRepository.save(staff);
                        
                        // Update counts
                        shiftStaffCounts.put(targetShift, shiftStaffCounts.get(targetShift) + 1);
                        
                        log.info("Moved unused staff {} from shift {} to shift {}", 
                                staffId, oldShift, targetShift);
                    }
                } else {
                    log.info("No other shifts need additional staff. Staff {} remains in shift {}", 
                            staffId, currentShift);
                }
            }
        }
        
        // Then, handle underutilized staff if needed
        if (!underutilizedStaffIds.isEmpty()) {
            log.info("Considering {} underutilized staff members from {} shift for reassignment", 
                    underutilizedStaffIds.size(), currentShift);
            
            // Only reassign underutilized staff if there's a major shortage elsewhere
            for (Shift shift : Shift.values()) {
                if (shift == currentShift) continue;
                
                int roomsPerStaff = SHIFT_DURATION_HOURS * 60 / DAILY_CLEANING_DURATION;
                int requiredStaff = (int) Math.ceil((double) shiftRoomCounts.get(shift) / roomsPerStaff);
                int availableStaff = shiftStaffCounts.get(shift);
                
                // If there's a significant shortage (more than 2 staff needed)
                if (requiredStaff > availableStaff + 2) {
                    for (Long staffId : underutilizedStaffIds) {
                        Optional<Staff> staffOpt = staffRepository.findById(staffId.toString());
                        if (staffOpt.isPresent()) {
                            Staff staff = staffOpt.get();
                            String oldShift = staff.getShiftId();
                            staff.setShiftId(shift.name());
                            staffRepository.save(staff);
                            
                            // Update counts
                            shiftStaffCounts.put(shift, shiftStaffCounts.get(shift) + 1);
                            
                            log.info("Moved underutilized staff {} from shift {} to shift {} due to high demand", 
                                    staffId, oldShift, shift);
                            
                            // If we've addressed the shortage, stop reassigning
                            if (shiftStaffCounts.get(shift) >= requiredStaff) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Find the shift that needs staff the most
     */
    private Shift findMostNeededShift(Map<Shift, Integer> shiftRoomCounts, Map<Shift, Integer> shiftStaffCounts) {
        Shift mostNeededShift = null;
        double highestNeed = -1;
        
        for (Shift shift : Shift.values()) {
            if (!shiftRoomCounts.containsKey(shift) || !shiftStaffCounts.containsKey(shift)) {
                continue;
            }
            
            int roomCount = shiftRoomCounts.get(shift);
            int staffCount = shiftStaffCounts.get(shift);
            
            if (roomCount == 0) {
                continue; // No rooms, no need for staff
            }
            
            int roomsPerStaff = SHIFT_DURATION_HOURS * 60 / DAILY_CLEANING_DURATION;
            int requiredStaff = (int) Math.ceil((double) roomCount / roomsPerStaff);
            
            // Calculate need as the ratio of required to available staff
            double needRatio = staffCount > 0 ? (double) requiredStaff / staffCount : Double.MAX_VALUE;
            
            // If this shift needs more staff than what we've found so far
            if (needRatio > highestNeed && requiredStaff > staffCount) {
                highestNeed = needRatio;
                mostNeededShift = shift;
            }
        }
        
        return mostNeededShift;
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
     * Redistributes staff across shifts based on needs before assignment happens.
     * This proactively moves staff from shifts with excess capacity to shifts with shortages.
     * 
     * @param date The date for which to redistribute staff
     * @param targetShift The shift that needs staff
     * @param requiredStaffCount How many staff are needed for the target shift
     */
    private void redistributeStaffAcrossShifts(LocalDate date, Shift targetShift, int requiredStaffCount) {
        log.info("STAFF REDISTRIBUTION: Analyzing all shifts to find staff for {}", targetShift);
        
        // Get counts for all shifts to determine availability
        Map<Shift, Integer> shiftRoomCounts = new HashMap<>();
        Map<Shift, Integer> shiftStaffCounts = new HashMap<>();
        Map<Shift, Integer> shiftRequiredStaff = new HashMap<>();
        Map<Shift, Integer> shiftExcessStaff = new HashMap<>();
        
        // Calculate requirements for the target shift
        shiftRequiredStaff.put(targetShift, requiredStaffCount);
        
        // Current staff for target shift
        List<Staff> targetShiftStaff = staffRepository.findByShiftIdAndIsActiveTrue(targetShift.name());
        shiftStaffCounts.put(targetShift, targetShiftStaff.size());
        
        // Calculate how many staff we need to move to target shift
        int staffShortage = requiredStaffCount - targetShiftStaff.size();
        
        if (staffShortage <= 0) {
            log.info("No staff redistribution needed for shift {}", targetShift);
            return;
        }
        
        log.info("Need to move {} staff to shift {}", staffShortage, targetShift);
        
        // Analyze all other shifts to find available staff
        for (Shift shift : Shift.values()) {
            if (shift == targetShift) {
                continue;
            }
            
            // Get room count for this shift
            List<RoomCleaningSchedule> shiftSchedules = roomCleaningScheduleRepository
                .findByDateAndShiftAndStatus(date, shift, CleaningStatus.PENDING);
            shiftRoomCounts.put(shift, shiftSchedules.size());
            
            // Get staff count for this shift
            List<Staff> shiftStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
            shiftStaffCounts.put(shift, shiftStaff.size());
            
            // Calculate required staff for this shift
            int roomsPerStaff = SHIFT_DURATION_HOURS * 60 / DAILY_CLEANING_DURATION;
            int requiredStaff = (int) Math.ceil((double) shiftSchedules.size() / roomsPerStaff);
            shiftRequiredStaff.put(shift, requiredStaff);
            
            // Calculate excess staff for this shift
            int excessStaff = shiftStaff.size() - requiredStaff;
            shiftExcessStaff.put(shift, Math.max(0, excessStaff));
            
            log.info("STAFF ANALYSIS - Shift {}: {} rooms, {} staff available, {} staff required, {} excess staff", 
                    shift, shiftSchedules.size(), shiftStaff.size(), requiredStaff, Math.max(0, excessStaff));
        }
        
        // Staff we've moved so far
        int staffMoved = 0;
        
        // First, try to take staff from shifts with excess capacity
        for (Shift shift : Shift.values()) {
            if (shift == targetShift) {
                continue;
            }
            
            int excessStaff = shiftExcessStaff.get(shift);
            
            if (excessStaff > 0) {
                // This shift has excess staff we can move
                int staffToMove = Math.min(excessStaff, staffShortage - staffMoved);
                
                if (staffToMove > 0) {
                    log.info("Moving {} excess staff from shift {} to shift {}", 
                            staffToMove, shift, targetShift);
                    
                    // Find staff to move
                    List<Staff> availableStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
                    
                    // Move up to staffToMove staff
                    int movedInThisIteration = moveStaffBetweenShifts(availableStaff, shift, targetShift, staffToMove);
                    staffMoved += movedInThisIteration;
                    
                    if (staffMoved >= staffShortage) {
                        log.info("Successfully moved {} staff to shift {}. Requirement met.", 
                                staffMoved, targetShift);
                        return;
                    }
                }
            }
        }
        
        // If we still need more staff, we'll have to take from shifts that may need them
        // but we'll prioritize shifts with the smallest shortages
        if (staffMoved < staffShortage) {
            // Sort shifts by their staff requirements (ascending)
            List<Shift> shiftsOrderedByNeed = Shift.values().length > 0 ? Arrays.asList(Shift.values()) : new ArrayList<>();
            shiftsOrderedByNeed.sort((a, b) -> {
                if (a == targetShift) return 1;
                if (b == targetShift) return -1;
                
                int aReq = shiftRequiredStaff.getOrDefault(a, 0);
                int aAvail = shiftStaffCounts.getOrDefault(a, 0);
                int aNeed = Math.max(0, aReq - aAvail);
                
                int bReq = shiftRequiredStaff.getOrDefault(b, 0);
                int bAvail = shiftStaffCounts.getOrDefault(b, 0);
                int bNeed = Math.max(0, bReq - bAvail);
                
                return Integer.compare(aNeed, bNeed);
            });
            
            for (Shift shift : shiftsOrderedByNeed) {
                if (shift == targetShift) {
                    continue;
                }
                
                int availableStaffCount = shiftStaffCounts.getOrDefault(shift, 0);
                
                if (availableStaffCount > 0) {
                    // Determine how many staff we can take without causing severe shortage
                    int requiredStaffForShift = shiftRequiredStaff.getOrDefault(shift, 0);
                    int maxStaffToTake = Math.max(0, availableStaffCount - Math.max(1, requiredStaffForShift / 2));
                    
                    int staffToMove = Math.min(maxStaffToTake, staffShortage - staffMoved);
                    
                    if (staffToMove > 0) {
                        log.info("Moving {} staff from shift {} to shift {} (this may impact the source shift)", 
                                staffToMove, shift, targetShift);
                        
                        // Find staff to move
                        List<Staff> availableStaff = staffRepository.findByShiftIdAndIsActiveTrue(shift.name());
                        
                        // Move up to staffToMove staff
                        int movedInThisIteration = moveStaffBetweenShifts(availableStaff, shift, targetShift, staffToMove);
                        staffMoved += movedInThisIteration;
                        
                        if (staffMoved >= staffShortage) {
                            log.info("Successfully moved {} staff to shift {}. Requirement met.", 
                                    staffMoved, targetShift);
                            return;
                        }
                    }
                }
            }
        }
        
        log.warn("Only moved {} of {} required staff to shift {}. Staff shortage may still exist.", 
                staffMoved, staffShortage, targetShift);
    }
    
    /**
     * Moves a specified number of staff from one shift to another.
     * 
     * @param availableStaff List of staff in the source shift
     * @param sourceShift The shift to move staff from
     * @param targetShift The shift to move staff to
     * @param staffToMove How many staff to move
     * @return The number of staff actually moved
     */
    private int moveStaffBetweenShifts(List<Staff> availableStaff, Shift sourceShift, Shift targetShift, int staffToMove) {
        int staffMoved = 0;
        
        for (int i = 0; i < availableStaff.size() && staffMoved < staffToMove; i++) {
            Staff staff = availableStaff.get(i);
            String oldShift = staff.getShiftId();
            staff.setShiftId(targetShift.name());
            staffRepository.save(staff);
            staffMoved++;
            
            log.info("Moved staff {} from shift {} to shift {}", 
                    staff.getStaffId(), oldShift, targetShift);
        }
        
        return staffMoved;
    }
} 