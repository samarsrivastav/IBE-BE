package backend.entity.enums;

public enum CleaningType {
    DAILY_CLEANING(30),    // 30 minutes
    DEEP_CLEANING(120);    // 120 minutes

    private final int durationInMinutes;

    CleaningType(int durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }
} 