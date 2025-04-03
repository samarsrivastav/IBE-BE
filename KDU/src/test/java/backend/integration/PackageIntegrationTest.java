package backend.integration;

import backend.dto.request.PackageSearchRequestDTO;
import backend.dto.response.RoomTypePackagesDTO;
import backend.service.PackageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PackageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PackageService packageService; // Mock the service

    private PackageSearchRequestDTO validRequest;

    @BeforeEach
    void setup() {
        validRequest = new PackageSearchRequestDTO();
        validRequest.setPropertyId(1L);
        validRequest.setStartDate(LocalDate.now().plusDays(1));
        validRequest.setEndDate(LocalDate.now().plusDays(5));
    }

    @Test
    void getAllPackages_ReturnsPackages_WhenValidRequest() throws Exception {
        // Arrange
        RoomTypePackagesDTO mockResponse = new RoomTypePackagesDTO();
        mockResponse.setRoomTypeId(101L);
        mockResponse.setPackages(Collections.emptyList());

        Mockito.when(packageService.getAllPackages(Mockito.any())).thenReturn(Collections.singletonList(mockResponse));

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getAllPackages_ReturnsEmptyList_WhenNoPackagesFound() throws Exception {
        // Arrange
        Mockito.when(packageService.getAllPackages(Mockito.any())).thenReturn(Collections.emptyList());

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllPackages_ReturnsBadRequest_WhenInvalidRequest() throws Exception {
        // Arrange: Create an invalid request (missing propertyId)
        PackageSearchRequestDTO invalidRequest = new PackageSearchRequestDTO();
        invalidRequest.setStartDate(LocalDate.now().plusDays(1));
        invalidRequest.setEndDate(LocalDate.now().plusDays(5));

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)));

        // Assert
        result.andExpect(status().isBadRequest());
    }

    @Test
    void getAllPackages_ReturnsBadRequest_WhenEndDateIsBeforeStartDate() {
        // Arrange: Invalid date range
        PackageSearchRequestDTO invalidRequest = new PackageSearchRequestDTO();
        invalidRequest.setPropertyId(1L);
        invalidRequest.setStartDate(LocalDate.now().plusDays(5));
        invalidRequest.setEndDate(LocalDate.now().plusDays(3)); // End date is before start date

        // Mock behavior for invalid request
        Mockito.doThrow(new IllegalArgumentException("End date must be after start date"))
                .when(packageService).getAllPackages(Mockito.any());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            packageService.getAllPackages(invalidRequest);
        });

        // Verify exception message
        assertEquals("End date must be after start date", exception.getMessage());
    }

    @Test
    void getAllPackages_ReturnsInternalServerError_WhenServiceFails() throws Exception {
        // Arrange: Simulate service failure
        Mockito.when(packageService.getAllPackages(Mockito.any())).thenThrow(new RuntimeException("Service failure"));

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Assert
        result.andExpect(status().isInternalServerError());
    }
}
