package backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class GraphQLResponse {
    @JsonProperty("data")
    private Map<String, Object> data;

    public Map<String, Object> getData() { return data; }
}
