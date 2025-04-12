package backend.repository;

import backend.entity.Staff;
import backend.entity.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    List<Staff> findByShiftIdAndIsActiveTrue(String shiftId);
    List<Staff> findByIsActiveTrue();
    int countByShiftIdAndIsActiveTrue(String shiftId);
} 