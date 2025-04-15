package backend.service.impl;

import backend.entity.RoomTypes;
import backend.repository.RoomTypeRepository;
import backend.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    @Override
    public List<RoomTypes> getAllRoomTypes() {
        log.info("Fetching all room types from database");
        return roomTypeRepository.findAll();
    }
} 