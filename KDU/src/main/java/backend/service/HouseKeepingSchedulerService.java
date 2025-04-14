package backend.service;

import backend.entity.Staff;
import backend.entity.enums.Shift;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public interface HouseKeepingSchedulerService {
    @Scheduled(cron = "0 0 8 * * ?") // Runs daily at 8 AM
    void generateDailySchedules();

    @Scheduled(cron = "0 0 7,11,15 * * ?") // Runs at 7 AM, 11 AM, and 3 PM
    void checkShiftStaffing();

    void sendStaffShortageEmail(String recipient, String shift, int shortageCount);
}