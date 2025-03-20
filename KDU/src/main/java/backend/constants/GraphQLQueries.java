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

    public static final String FIND_PRICE_BY_DATE= "{ \"query\": \"{ listProperties(where: {property_id: {equals: 15 }}) { room_type { room_rates { room_rate { basic_nightly_rate date room_rate_id } } } } }\"}";
}
