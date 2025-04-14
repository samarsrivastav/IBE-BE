package backend.service.impl;


import backend.entity.enums.Shift;
import backend.model.RoomCleaningSchedule;
import backend.service.EmailService;
import backend.service.HouseCleaningService;
import backend.service.HouseKeepingSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HouseKeepingSchedulerServiceImpl implements HouseKeepingSchedulerService {

    private final HouseCleaningService houseCleaningService;
    private final EmailService emailService;

    @Override
    @Scheduled(cron = "0 5 16 * * ?") // Runs daily at 16:01
    public void generateDailySchedules() {
        log.info("Generating daily cleaning schedules at 16:01");
        LocalDate currentDate = LocalDate.parse("2025-05-01");
        houseCleaningService.generateCleaningSchedules(currentDate);
    }

    @Override
    @Scheduled(cron = "0 6 16 * * ?") // Runs at 16:01 for morning shift
    public void assignMorningShiftStaff() {
        log.info("Assigning staff to MORNING shift at 16:01");
        LocalDate currentDate = LocalDate.parse("2025-05-01");
        List<RoomCleaningSchedule> schedules = houseCleaningService.assignStaffToSchedules(currentDate, Shift.MORNING_SHIFT);
        log.info("Completed morning shift staff assignment: {} schedules processed", schedules.size());
    }

    @Scheduled(cron = "0 7 16 * * ?") // Runs at 16:02 for afternoon shift
    public void assignAfternoonShiftStaff() {
        log.info("Assigning staff to AFTERNOON shift at 16:02");
        LocalDate currentDate = LocalDate.parse("2025-05-01"); // LocalDate.now();
        List<RoomCleaningSchedule> schedules = houseCleaningService.assignStaffToSchedules(currentDate, Shift.AFTERNOON_SHIFT);
        log.info("Completed afternoon shift staff assignment: {} schedules processed", schedules.size());
    }

    @Scheduled(cron = "0 8 16 * * ?") // Runs at 16:03 for evening shift
    public void assignEveningShiftStaff() {
        log.info("Assigning staff to EVENING shift at 16:03");
        LocalDate currentDate = LocalDate.parse("2025-05-01");
        List<RoomCleaningSchedule> schedules = houseCleaningService.assignStaffToSchedules(currentDate, Shift.EVENING_SHIFT);
        log.info("Completed evening shift staff assignment: {} schedules processed", schedules.size());
    }


}