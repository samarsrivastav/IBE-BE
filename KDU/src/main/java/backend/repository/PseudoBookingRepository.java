package backend.repository;

import backend.model.PseudoBooking;
import backend.model.PseudoBookingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Repository
public interface PseudoBookingRepository extends JpaRepository<PseudoBooking, PseudoBookingId> {

    @Query("SELECT COUNT(p) FROM PseudoBooking p WHERE p.pseudoBookingId.roomId = :roomId AND p.pseudoBookingId.bookingDate BETWEEN :startDate AND :endDate")
    int countBookingsForRoomInDateRange(@Param("roomId") Integer roomId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM PseudoBooking p WHERE p.bookingGroupId = :bookingGroupId")
    void deleteByBookingGroupId(@Param("bookingGroupId") long bookingGroupId);
}