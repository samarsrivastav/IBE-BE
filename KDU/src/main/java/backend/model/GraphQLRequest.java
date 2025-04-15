package backend.model;

import java.util.Map;

public class GraphQLRequest {
    private final String query;
    private final Map<String, Object> variables;

    public GraphQLRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }

    public String getQuery() { return query; }
    public Map<String, Object> getVariables() { return variables; }
}
