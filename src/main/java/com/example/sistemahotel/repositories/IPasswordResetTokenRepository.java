package com.example.sistemahotel.repositories;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.PasswordResetToken;

@Repository
public interface IPasswordResetTokenRepository extends CrudRepository<PasswordResetToken, Integer> {
    public Optional<PasswordResetToken> findByToken(String token);
}
