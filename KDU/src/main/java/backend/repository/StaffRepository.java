package backend.repository;

import backend.entity.Staff;
import backend.entity.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    List<Staff> findByShiftIdAndIsActiveTrue(String shiftId);
    List<Staff> findByIsActiveTrue();
    int countByShiftIdAndIsActiveTrue(String shiftId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Staff s WHERE s.is_third_party = true")
    int deleteAllThirdPartyStaff();
    
    List<Staff> findByIsThirdPartyTrue();
} 