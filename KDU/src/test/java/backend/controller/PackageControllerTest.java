package backend.controller;

import backend.dto.request.PackageSearchRequestDTO;
import backend.dto.response.PackageResponseDTO;
import backend.dto.response.RoomTypePackagesDTO;
import backend.service.PackageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PackageController.class)
class PackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PackageService packageService;

    private PackageSearchRequestDTO searchRequest;

    @BeforeEach
    void setUp() {
        searchRequest = new PackageSearchRequestDTO();
        searchRequest.setPropertyId(1L);
        searchRequest.setStartDate(LocalDate.now());
        searchRequest.setEndDate(LocalDate.now().plusDays(2));
    }

    @Test
    void getAllPackages_WithValidRequest_ReturnsPackages() throws Exception {
        // Arrange
        RoomTypePackagesDTO response = createSampleResponse();
        when(packageService.getAllPackages(any())).thenReturn(Arrays.asList(response));

        // Act & Assert
        mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].roomTypeId").value(1))
            .andExpect(jsonPath("$[0].packages[0].id").value("standard-1"))
            .andExpect(jsonPath("$[0].packages[0].type").value("standard"));
    }

    @Test
    void getAllPackages_WithEmptyResponse_ReturnsEmptyList() throws Exception {
        // Arrange
        when(packageService.getAllPackages(any())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }


    @Test
    void getAllPackages_WithMissingRequiredFields_ReturnsBadRequest() throws Exception {
        // Arrange
        PackageSearchRequestDTO invalidRequest = new PackageSearchRequestDTO();
        invalidRequest.setStartDate(LocalDate.now());

        // Act & Assert
        mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    private RoomTypePackagesDTO createSampleResponse() {
        RoomTypePackagesDTO response = new RoomTypePackagesDTO();
        response.setRoomTypeId(1L);
        response.setPackages(Arrays.asList(
            createPackageResponseDTO("standard-1", "Standard Rate", "Standard room rate with basic amenities", "100.00", "standard")
        ));
        return response;
    }

    private PackageResponseDTO createPackageResponseDTO(String id, String title, String description, String price, String type) {
        PackageResponseDTO dto = new PackageResponseDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setPrice(price);
        dto.setType(type);
        return dto;
    }
} 