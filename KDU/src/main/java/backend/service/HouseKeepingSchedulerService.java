package backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public interface HouseKeepingSchedulerService {
    @Scheduled(cron = "0 0 16 * * ?") // Runs daily at 8 AM
    void generateDailySchedules();

    @Scheduled(cron = "0 3 16 * * ?") // Runs at 16:03 for evening shift
    public void assignEveningShiftStaff();

    @Scheduled(cron = "0 2 16 * * ?") // Runs at 16:02 for afternoon shift
    public void assignAfternoonShiftStaff();

    @Scheduled(cron = "0 1 16 * * ?") // Runs at 16:01 for morning shift
    public void assignMorningShiftStaff();
}