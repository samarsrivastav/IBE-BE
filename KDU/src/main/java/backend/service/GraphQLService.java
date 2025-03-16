package backend.service;

import backend.constants.GraphQLQueries;
import backend.model.GraphQLRequest;
import backend.model.GraphQLResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class GraphQLService {

    @Value("${graphql.endpoint}")
    private String graphqlEndpoint;

    @Value("${graphql.apiKey}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Object fetchPropertyByName(String propertyName) {
        String query = GraphQLQueries.FIND_PROPERTY_BY_NAME.formatted(propertyName);

        GraphQLRequest request = new GraphQLRequest(query, Collections.emptyMap());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);

        HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GraphQLResponse> response = restTemplate.exchange(
                graphqlEndpoint, HttpMethod.POST, entity, GraphQLResponse.class
        );

        return response.getBody() != null ? response.getBody().getData() : null;
    }
}
