package IBE_Backend.repository;

//import com.example.propertymanagement.model.Property;
import IBE_Backend.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
}
