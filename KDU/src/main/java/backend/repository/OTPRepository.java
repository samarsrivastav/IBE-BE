package backend.repository;


import backend.entity.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository

public interface OTPRepository extends JpaRepository<OTP, Long> {

    @Modifying
    @Query("Select o from OTP o where o.email  and o.otp = ?2")
    Optional<OTP> findTopByEmailOrderByCreatedAtDesc(String email);
}