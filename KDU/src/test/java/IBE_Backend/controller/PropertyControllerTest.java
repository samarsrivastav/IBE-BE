package IBE_Backend.controller;

import IBE_Backend.dto.PropertyDTO;
import IBE_Backend.service.PropertyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PropertyController.class)
public class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyService propertyService;

    @Autowired
    private ObjectMapper objectMapper;

    private PropertyDTO propertyDTO1;
    private PropertyDTO propertyDTO2;
    private List<PropertyDTO> propertyDTOList;

    @BeforeEach
    void setUp() {
        propertyDTO1 = new PropertyDTO(1L, "Test Property 1", "123 Test Street");
        propertyDTO2 = new PropertyDTO(2L, "Test Property 2", "456 Test Avenue");
        propertyDTOList = Arrays.asList(propertyDTO1, propertyDTO2);
    }

    @Test
    void testGetAllProperties() throws Exception {
        when(propertyService.getAllProperties()).thenReturn(propertyDTOList);

        mockMvc.perform(get("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Property 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Test Property 2"));

        verify(propertyService, times(1)).getAllProperties();
    }

    @Test
    void testGetPropertyById() throws Exception {
        when(propertyService.getPropertyById(1L)).thenReturn(propertyDTO1);

        mockMvc.perform(get("/api/properties/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Property 1"))
                .andExpect(jsonPath("$.address").value("123 Test Street"));

        verify(propertyService, times(1)).getPropertyById(1L);
    }

    @Test
    void testCreateProperty() throws Exception {
        when(propertyService.createProperty(any(PropertyDTO.class))).thenReturn(propertyDTO1);

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propertyDTO1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Property 1"));

        verify(propertyService, times(1)).createProperty(any(PropertyDTO.class));
    }

    @Test
    void testUpdateProperty() throws Exception {
        PropertyDTO updatedPropertyDTO = new PropertyDTO(1L, "Updated Property", "Updated Address");
        when(propertyService.updateProperty(eq(1L), any(PropertyDTO.class))).thenReturn(updatedPropertyDTO);

        mockMvc.perform(put("/api/properties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPropertyDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Property"))
                .andExpect(jsonPath("$.address").value("Updated Address"));

        verify(propertyService, times(1)).updateProperty(eq(1L), any(PropertyDTO.class));
    }

    @Test
    void testDeleteProperty() throws Exception {
        doNothing().when(propertyService).deleteProperty(1L);

        mockMvc.perform(delete("/api/properties/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(propertyService, times(1)).deleteProperty(1L);
    }
}