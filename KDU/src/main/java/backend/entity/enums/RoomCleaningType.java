package backend.entity.enums;

public enum RoomCleaningType {
    CHECKIN_CHECKOUT,  // Both check-in and check-out on the same day
    CHECKIN_ONLY,      // Only check-in on the day
    CHECKOUT_ONLY,     // Only check-out on the day
    BOOKED_NO_CHANGE   // Booked but no check-in or check-out on the day
} 