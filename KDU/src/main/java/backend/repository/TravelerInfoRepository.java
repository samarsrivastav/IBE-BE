package backend.repository;

import backend.entity.TravelerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelerInfoRepository extends JpaRepository<TravelerInfo, Long> {
} 