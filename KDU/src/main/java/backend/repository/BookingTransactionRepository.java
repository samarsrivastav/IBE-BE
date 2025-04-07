package backend.repository;

import backend.model.BookingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingTransactionRepository extends JpaRepository<BookingTransaction, Long> {
        Optional<BookingTransaction> findByConfirmationId(UUID uuid);
        Optional<BookingTransaction> findById(Long id);
        Optional<List<BookingTransaction>> findByEmail(String email);

        @Query(value = "SELECT * FROM booking_transaction WHERE booking_details->'confirmationDetails'->>'endDate' = :checkOutDate", nativeQuery = true)
        List<BookingTransaction> findByCheckOutDate(@Param("checkOutDate") String checkOutDate);
}