package IBE_Backend.integration;

import IBE_Backend.dto.PropertyDTO;
import IBE_Backend.entity.Property;
import IBE_Backend.repository.PropertyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PropertyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Property testProperty;

    @BeforeEach
    void setUp() {
        propertyRepository.deleteAll();

        testProperty = new Property("Integration Test Property", "789 Test Blvd");
        propertyRepository.save(testProperty);
    }

    @Test
    void testGetAllProperties() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Integration Test Property"));
    }

    @Test
    void testGetPropertyById() throws Exception {
        mockMvc.perform(get("/api/properties/" + testProperty.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Property"))
                .andExpect(jsonPath("$.address").value("789 Test Blvd"));
    }

    @Test
    void testCreateProperty() throws Exception {
        PropertyDTO newPropertyDTO = new PropertyDTO(null, "New Test Property", "999 New Street");

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPropertyDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Test Property"))
                .andExpect(jsonPath("$.address").value("999 New Street"));
    }

    @Test
    void testUpdateProperty() throws Exception {
        PropertyDTO updateDTO = new PropertyDTO(
                testProperty.getId(),
                "Updated Integration Property",
                "789 Updated Blvd"
        );

        mockMvc.perform(put("/api/properties/" + testProperty.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Integration Property"))
                .andExpect(jsonPath("$.address").value("789 Updated Blvd"));
    }

    @Test
    void testDeleteProperty() throws Exception {
        mockMvc.perform(delete("/api/properties/" + testProperty.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify property no longer exists
        mockMvc.perform(get("/api/properties/" + testProperty.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}