package backend.service;

import backend.dto.request.PackageSearchRequestDTO;
import backend.dto.response.RoomTypePackagesDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock
    private PackageService packageService;

    @BeforeEach
    void setUp() {
        packageService = mock(PackageService.class);
    }

    @Test
    void testGetAllPackages() {
        // Arrange
        PackageSearchRequestDTO searchRequest = new PackageSearchRequestDTO();
        RoomTypePackagesDTO package1 = new RoomTypePackagesDTO();
        RoomTypePackagesDTO package2 = new RoomTypePackagesDTO();
        List<RoomTypePackagesDTO> mockResponse = Arrays.asList(package1, package2);

        when(packageService.getAllPackages(searchRequest)).thenReturn(mockResponse);

        // Act
        List<RoomTypePackagesDTO> result = packageService.getAllPackages(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(packageService, times(1)).getAllPackages(searchRequest);
    }
}
