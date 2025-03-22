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

    public static String getFindPriceByDateQuery(int propertyId) {
        return String.format("""
                { "query": "{ listProperties(where: {property_id: {equals: %d }}) { room_type { room_rates { room_rate { basic_nightly_rate date room_rate_id } } } } }" }
                """, propertyId);
    }
}
