package backend.constants;

import java.util.List;
import java.util.stream.Collectors;

public class HouseKeeping {

    public static String getBookedRoomsInDateRangeQuery(int propertyId, String date, List<Integer> roomTypeIds) {
        String roomTypeIdsString = roomTypeIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return String.format("""
            query BookedRoomsInDateRange {
              listRoomAvailabilities(
                where: {
                  property_id: { equals: %d }
                  booking_id: { not: { equals: 0 } }
                  date: {
                    equals: "%s"
                  }
                  room: {
                    room_type_id: { in: [%s] }
                  }
                }
                take: 10000
              ) {
                availability_id
                room_id
                booking_id
                date
              }
            }
            """, propertyId, date, roomTypeIdsString);
    }
}
