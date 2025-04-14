//package backend.service.impl;
//
//import backend.entity.Staff;
//import backend.entity.RoomCleaningSchedule;
//import backend.entity.enums.Shift;
//import backend.entity.enums.CleaningStatus;
//import backend.service.EmailService;
//import backend.service.HouseCleaningService;
//import backend.service.HouseKeepingSchedulerService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.List;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class HouseKeepingSchedulerServiceImpl implements HouseKeepingSchedulerService {
//
//    private final HouseCleaningService houseCleaningService;
//    private final EmailService emailService;
//
//    @Override
//    @Scheduled(cron = "0 0 8 * * ?") // Runs daily at 8 AM
//    public void generateDailySchedules() {
//        log.info("Generating daily cleaning schedules at 8 AM");
//        houseCleaningService.generateCleaningSchedules(LocalDate.now());
//    }
//
//    @Override
//    @Scheduled(cron = "0 0 7,11,15 * * ?") // Runs at 7 AM, 11 AM, and 3 PM
//    public void checkShiftStaffing() {
//        log.info("Checking staff availability for shifts");
//        LocalDate currentDate = LocalDate.now();
//
//        // Check each shift
//        for (Shift shift : Shift.values()) {
//            try {
//                assignStaffToShift(shift, currentDate);
//            } catch (CriticalStaffShortageException e) {
//                // This will catch our custom exception when no staff is available after redistribution
//                sendCriticalStaffShortageEmail("admin@example.com", shift.name(), e.getPendingSchedules().size(), currentDate);
//            }
//        }
//    }
//
//    private List<RoomCleaningSchedule> assignStaffToShift(Shift shift, LocalDate date) {
//        log.info("Assigning staff to {} shift for date {}", shift, date);
//
//        try {
//            List<RoomCleaningSchedule> schedules = houseCleaningService.assignStaffToSchedules(date, shift);
//
//            // Check if there are any unassigned schedules (staff is null)
//            long unassignedCount = schedules.stream()
//                    .filter(s -> s.getStatus() == CleaningStatus.PENDING && s.getStaffId() == null)
//                    .count();
//
//            if (unassignedCount > 0) {
//                sendStaffShortageEmail("admin@example.com", shift.name(), (int)unassignedCount);
//            }
//
//            return schedules;
//        } catch (RuntimeException e) {
//            // If there's an issue with staff assignment, still send an email
//            log.error("Error assigning staff to shift {}: {}", shift, e.getMessage());
//            sendStaffShortageEmail("admin@example.com", shift.name(), 0);
//            throw e;
//        }
//    }
//
//    @Override
//    public void sendStaffShortageEmail(String recipient, String shift, int shortageCount) {
//        String subject = "Staff Shortage Alert - " + shift + " Shift";
//        String body = String.format("""
//                <html>
//                    <body>
//                        <h3>Staff Shortage Alert</h3>
//                        <p>There is a shortage of staff for the %s shift:</p>
//                        <ul>
//                            <li>Shift: %s</li>
//                            <li>Shortage Count: %d staff</li>
//                            <li>Date: %s</li>
//                        </ul>
//                        <p>Please take immediate action to address this shortage.</p>
//                        <p>Thank you,</p>
//                        <p>Housekeeping Management</p>
//                    </body>
//                </html>
//                """,
//                shift, shift, shortageCount, LocalDate.now().toString());
//
//        emailService.sendEmail(recipient, subject, body);
//        log.info("Sent staff shortage email for {} shift to {}", shift, recipient);
//    }
//
//    /**
//     * Sends a critical staff shortage email when no staff is available for a shift after redistribution
//     */
//    public void sendCriticalStaffShortageEmail(String recipient, String shift, int roomCount, LocalDate date) {
//        String subject = "CRITICAL STAFF SHORTAGE ALERT - " + shift + " Shift";
//
//        // Calculate the minimum required staff based on room count
//        int requiredStaff = houseCleaningService.getRequiredStaffForShift(Shift.valueOf(shift));
//
//        String body = String.format("""
//                <html>
//                    <body>
//                        <h2 style="color: red;">CRITICAL STAFF SHORTAGE ALERT</h2>
//                        <p><strong>URGENT ACTION REQUIRED:</strong> No staff is available for the %s shift after redistribution attempts.</p>
//
//                        <h3>Shift Details:</h3>
//                        <ul>
//                            <li>Shift: %s</li>
//                            <li>Date: %s</li>
//                            <li>Rooms requiring cleaning: %d</li>
//                            <li>Minimum required staff: %d</li>
//                        </ul>
//
//                        <h3>Impact:</h3>
//                        <p>All %d rooms scheduled for cleaning during this shift will remain uncleaned unless immediate action is taken.</p>
//
//                        <h3>Recommended Actions:</h3>
//                        <ol>
//                            <li>Contact off-duty staff for emergency coverage</li>
//                            <li>Consider temporary staffing options</li>
//                            <li>Evaluate possibility of rescheduling critical rooms to other shifts</li>
//                            <li>Notify management of service disruption risk</li>
//                        </ol>
//
//                        <p style="color: red; font-weight: bold;">This is a system-generated critical alert that requires immediate attention.</p>
//
//                        <p>Thank you,</p>
//                        <p>Housekeeping Management System</p>
//                    </body>
//                </html>
//                """,
//                shift, shift, date.toString(), roomCount, requiredStaff, roomCount);
//
//        emailService.sendEmail(recipient, subject, body);
//        log.info("Sent CRITICAL staff shortage email for {} shift to {}", shift, recipient);
//    }
//}