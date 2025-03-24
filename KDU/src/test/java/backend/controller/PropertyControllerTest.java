package backend.controller;

import backend.dto.PropertyDTO;
import backend.service.PropertyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PropertyControllerTest {

    @Mock
    private PropertyService propertyService;

    @InjectMocks
    private PropertyController propertyController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(propertyController).build();
    }

    @Test
    void testGetAllProperties() throws Exception {
        List<PropertyDTO> properties = Arrays.asList(
                new PropertyDTO(1L, "Property One", "Address One"),
                new PropertyDTO(2L, "Property Two", "Address Two")
        );

        when(propertyService.getAllProperties()).thenReturn(properties);

        mockMvc.perform(get("/api/v1/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));

        verify(propertyService, times(1)).getAllProperties();
    }

    @Test
    void testGetPropertyById() throws Exception {
        PropertyDTO propertyDTO = new PropertyDTO(1L, "Property One", "Address One");

        when(propertyService.getPropertyById(1L)).thenReturn(propertyDTO);

        mockMvc.perform(get("/api/v1/properties/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Property One"));

        verify(propertyService, times(1)).getPropertyById(1L);
    }

    @Test
    void testCreateProperty() throws Exception {
        PropertyDTO propertyDTO = new PropertyDTO(null, "New Property", "New Address");
        PropertyDTO createdProperty = new PropertyDTO(1L, "New Property", "New Address");

        when(propertyService.createProperty(any(PropertyDTO.class))).thenReturn(createdProperty);

        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propertyDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Property"));

        verify(propertyService, times(1)).createProperty(any(PropertyDTO.class));
    }

    @Test
    void testUpdateProperty() throws Exception {
        PropertyDTO updatedProperty = new PropertyDTO(1L, "Updated Name", "Updated Address");

        when(propertyService.updateProperty(eq(1L), any(PropertyDTO.class))).thenReturn(updatedProperty);

        mockMvc.perform(put("/api/v1/properties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedProperty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));

        verify(propertyService, times(1)).updateProperty(eq(1L), any(PropertyDTO.class));
    }

    @Test
    void testDeleteProperty() throws Exception {
        doNothing().when(propertyService).deleteProperty(1L);

        mockMvc.perform(delete("/api/v1/properties/1"))
                .andExpect(status().isNoContent());

        verify(propertyService, times(1)).deleteProperty(1L);
    }
}
