package backend.repository;

import backend.model.SpecialOffers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialOffersRepository extends JpaRepository<SpecialOffers, Long> {
} 