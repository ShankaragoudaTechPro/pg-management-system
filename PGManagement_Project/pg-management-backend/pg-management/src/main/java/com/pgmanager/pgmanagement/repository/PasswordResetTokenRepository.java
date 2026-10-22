package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.PasswordResetToken;
import com.pgmanager.pgmanagement.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findTopByOwnerOrderByCreatedAtDesc(
            Owner owner
    );
}