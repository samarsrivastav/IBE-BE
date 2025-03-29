package backend.constants;

public class GraphQLQueries {
    private GraphQLQueries() {
    }

    public static final String FIND_PROPERTY_BY_NAME = """
            query FindPropertyByName {
                getProperty(where: { property_name: "%s" }) {
                    property_id
                    property_name
                    property_address
                    contact_number
                }
            }
            """;
    public static final String GET_PROPERTIES_BY_TENANT_ID = """
            query GetPropertiesByTenant {
                listProperties(where : { tenant_id: { equals: %d }}) {
                    property_id
                    property_name
                    property_address
                    contact_number
                }
            }
            """;

    public static String getFindPriceByDateQuery(int propertyId) {
        return String.format("""
                { "query": "{ listProperties(where: {property_id: {equals: %d }}) { room_type { room_rates { room_rate { basic_nightly_rate date room_rate_id } } } } }" }
                """, propertyId);
    }

    public static String getRoomTypeByDate(int propertyId, String startDate, String endDate, int maxCapacity) {
        return String.format("""
        query ListRoomAvailabilities {
            listRoomAvailabilities(
                where: {
                    property_id: { equals: %d },
                    booking: { booking_status: { status: { not: { equals: "BOOKED" }}} },
                    date: { gte: "%s", lte: "%s" },
                    room: { room_type: { max_capacity: { gte: %d }} }
                }
                take: 1000000
            ) {
                room {
                    room_type {
                        room_type_id
                        room_type_name
                        area_in_square_feet
                        double_bed
                        max_capacity
                        single_bed
                    }
                }
            }
        }
    """, propertyId, startDate, endDate, maxCapacity);
    }

    public static final String GET_PROMOTIONS_QUERY = """
        query ListPromotions {
            listPromotions(orderBy: {price_factor: ASC}) {
                promotion_id
                promotion_title
                promotion_description
                price_factor
                minimum_days_of_stay
                is_deactivated
            }
        }
    """;

    public static String getRoomRatesByRoomTypes(String roomTypeIds, String startDate, String endDate) {
        return String.format("""
        query GetRoomRates {
            listRoomRateRoomTypeMappings(
                where: {
                    room_type_id: {in: [%s]},
                    room_rate: {date: {gte: "%s", lte: "%s"}}
                }
                orderBy: {room_rate: {date: ASC}}
                take: 1000000
            ) {
                room_rate {
                    basic_nightly_rate
                    date
                    room_rate_id
                }
                room_type_id
            }
        }
        """, roomTypeIds, startDate, endDate);
    }
}

//public static String getRoomTypeByDate(int propertyId, String startDate, String endDate, int maxCapacity) {


 