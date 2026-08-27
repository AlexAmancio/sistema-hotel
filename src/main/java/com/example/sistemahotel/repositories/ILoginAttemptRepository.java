package com.example.sistemahotel.repositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.sistemahotel.modelos.LoginAttempt;

public interface ILoginAttemptRepository extends JpaRepository<LoginAttempt, Integer> {
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.successful = false AND la.attemptTime > :cutoffTime")
    long countFailedAttempts(@Param("ipAddress") String ipAddress, @Param("cutoffTime") LocalDateTime cutoffTime);
}
