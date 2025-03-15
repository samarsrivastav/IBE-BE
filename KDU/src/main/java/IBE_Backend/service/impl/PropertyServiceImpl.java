package IBE_Backend.service.impl;

import IBE_Backend.entity.Property;
import IBE_Backend.dto.PropertyDTO;
import IBE_Backend.exception.ResourceNotFoundException;
import IBE_Backend.repository.PropertyRepository;
import IBE_Backend.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;

    @Autowired
    public PropertyServiceImpl(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public List<PropertyDTO> getAllProperties() {
        return propertyRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PropertyDTO getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        return convertToDTO(property);
    }

    @Override
    public PropertyDTO createProperty(PropertyDTO propertyDTO) {
        Property property = convertToEntity(propertyDTO);
        Property savedProperty = propertyRepository.save(property);
        return convertToDTO(savedProperty);
    }

    @Override
    public PropertyDTO updateProperty(Long id, PropertyDTO propertyDTO) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        property.setName(propertyDTO.getName());
        property.setAddress(propertyDTO.getAddress());

        Property updatedProperty = propertyRepository.save(property);
        return convertToDTO(updatedProperty);
    }

    @Override
    public void deleteProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        propertyRepository.delete(property);
    }

    // Helper methods to convert between Entity and DTO
    private PropertyDTO convertToDTO(Property property) {
        return new PropertyDTO(
                property.getId(),
                property.getName(),
                property.getAddress()
        );
    }

    private Property convertToEntity(PropertyDTO propertyDTO) {
        Property property = new Property();
        property.setName(propertyDTO.getName());
        property.setAddress(propertyDTO.getAddress());
        // Don't set ID for new entities
        if (propertyDTO.getId() != null) {
            property.setId(propertyDTO.getId());
        }
        return property;
    }
}