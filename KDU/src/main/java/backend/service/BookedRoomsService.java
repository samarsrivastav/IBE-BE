package backend.service;

import java.util.List;

public interface BookedRoomsService {
    List<Long> getCheckInRoomsForToday(int propertyId);
    List<Long> getCheckOutRoomsForToday(int propertyId);
    List<Long> getCurrentlyBookedRooms(int propertyId);
} 