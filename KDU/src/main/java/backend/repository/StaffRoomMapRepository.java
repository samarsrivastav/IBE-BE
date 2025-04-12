package backend.repository;

import backend.entity.StaffRoomMap;
import backend.entity.enums.CleaningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRoomMapRepository extends JpaRepository<StaffRoomMap, Long> {
    List<StaffRoomMap> findByStaffStaffId(Long staffId);
    List<StaffRoomMap> findByRoomId(String roomId);
    List<StaffRoomMap> findByCleaningStatus(CleaningStatus status);
    List<StaffRoomMap> findByStaffStaffIdAndCleaningStatus(Long staffId, CleaningStatus status);
} 