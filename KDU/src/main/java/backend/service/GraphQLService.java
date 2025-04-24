package backend.service;

import backend.constants.GraphQLQueries;
import backend.model.GraphQLRequest;
import backend.model.GraphQLResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Service
public class GraphQLService {
    private static final Logger logger = LoggerFactory.getLogger(GraphQLService.class);

    @Value("${graphql.endpoint}")
    private String graphqlEndpoint;

    @Value("${graphql.apiKey}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Object fetchPropertyByName(String propertyName) {
        logger.info("Fetching property details for propertyName: {}", propertyName);

        String query = GraphQLQueries.FIND_PROPERTY_BY_NAME.formatted(propertyName);
        GraphQLRequest request = new GraphQLRequest(query, Collections.emptyMap());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);

        HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);


        try {
            ResponseEntity<GraphQLResponse> response = restTemplate.exchange(
                    graphqlEndpoint, HttpMethod.POST, entity, GraphQLResponse.class
            );

            if (response.getBody() != null) {
                logger.info("Successfully fetched property details for propertyName: {}", propertyName);
                return response.getBody().getData();
            } else {
                logger.warn("Received empty response while fetching property details for propertyName: {}", propertyName);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching property details for propertyName: {} - {}", propertyName, e.getMessage(), e);
            return null;
        }
    }

    @Cacheable(value = "propertiesCache", key = "#tenantId")
    public Object fetchAllProperties(Integer tenantId) {
        logger.info("Fetching all properties for tenantId: {}", tenantId);

        String query = GraphQLQueries.GET_PROPERTIES_BY_TENANT_ID.formatted(tenantId);
        GraphQLRequest request = new GraphQLRequest(query, Collections.emptyMap());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);

        HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GraphQLResponse> response = restTemplate.exchange(
                    graphqlEndpoint, HttpMethod.POST, entity, GraphQLResponse.class
            );

            if (response.getBody() != null) {
                logger.info("Successfully fetched properties for tenantId: {}", tenantId);
                return response.getBody().getData();
            } else {
                logger.warn("Received empty response while fetching properties for tenantId: {}", tenantId);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching properties for tenantId: {} - {}", tenantId, e.getMessage(), e);
            return null;
        }
    }
}
