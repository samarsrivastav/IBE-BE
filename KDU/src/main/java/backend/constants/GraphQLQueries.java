package backend.constants;

public class GraphQLQueries {
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
}
