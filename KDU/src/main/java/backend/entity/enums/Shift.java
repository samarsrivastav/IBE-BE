package backend.entity.enums;

public enum Shift {
    MORNING_SHIFT("5:00-9:00"),
    AFTERNOON_SHIFT("9:00-13:00"),
    EVENING_SHIFT("13:00-17:00");

    private final String timeRange;

    Shift(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getTimeRange() {
        return timeRange;
    }
} 