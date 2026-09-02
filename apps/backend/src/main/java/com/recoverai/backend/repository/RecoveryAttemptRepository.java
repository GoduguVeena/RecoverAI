package com.recoverai.backend.repository;

import com.recoverai.backend.domain.entity.RecoveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecoveryAttemptRepository extends JpaRepository<RecoveryAttempt, UUID> {
    List<RecoveryAttempt> findByRecoveryCaseId(UUID recoveryCaseId);
}
