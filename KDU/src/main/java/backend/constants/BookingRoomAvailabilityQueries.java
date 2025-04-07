package backend.constants;


import backend.dto.request.ConfirmationDetailsDto;

public class BookingRoomAvailabilityQueries {

    public static String getBookingsRoomAvailableQuery(long propertyId, String startDate, String endDate, long roomTypeId) {
        return String.format("""
        query BookingsRoomAvailable {
          listRoomAvailabilities(
            where: {
              property_id: { equals: %d },
              booking_id: { equals: 0 },
              date: { gte: "%s", lte: "%s" },
              room: { room_type_id: { equals: %d } }
            }
            take: 10000
          ) {
            availability_id
            room_id
          }
        }
        """, propertyId, startDate, endDate, roomTypeId);
    }

    public static String getBookingsMutationQuery(ConfirmationDetailsDto confirmationDetailsDto, String name) {
        String startDate = confirmationDetailsDto.getStartDate() + "T00:00:00.000Z";
        String endDate = confirmationDetailsDto.getEndDate() + "T00:00:00.000Z";

        return String.format("""
        mutation CreateBooking {
          createBooking(
            data: {
              check_in_date: "%s",
              check_out_date: "%s",
              adult_count: %d,
              child_count: %d,
              total_cost: %d,
              amount_due_at_resort: %d,
              booking_status: { connect: { status_id: 1 } },
              guest: { create: { guest_name: "%s" } },
              property_booked: { connect: { property_id: %d } }
            }
          ) {
            booking_id
          }
        }
        """,
                startDate,
                endDate,
                confirmationDetailsDto.getAdultCount(),
                confirmationDetailsDto.getChildCount(),
                (int) confirmationDetailsDto.getTotalCost(),
                (int) confirmationDetailsDto.getAmountDueAtResort(),
                name,
                confirmationDetailsDto.getPropertyId()
        );
    }

    public static String queryOfUpdatingRoomAvailability(long availabilityId, long bookingId) {
        return String.format("""
        mutation UpdateRoomAvailability {
          updateRoomAvailability(
            where: { availability_id: %d }
            data: { booking: { connect: { booking_id: %d } } }
          ) {
            booking_id
            availability_id
          }
        }
        """, availabilityId, bookingId);
    }

    public static String queryToUpdateTheBookingStatus(long bookingId) {
        return String.format("""
        mutation UpdateBookingStatus {
          updateBooking(
            where: { booking_id: %d }
            data: { booking_status: { connect: { status_id: 2 } } }
          ) {
            booking_id
          }
        }
        """, bookingId);
    }
}