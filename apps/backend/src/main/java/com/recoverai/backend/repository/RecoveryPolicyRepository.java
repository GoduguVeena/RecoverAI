package com.recoverai.backend.repository;

import com.recoverai.backend.domain.entity.RecoveryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryPolicyRepository extends JpaRepository<RecoveryPolicy, UUID> {
    Optional<RecoveryPolicy> findByMerchantId(UUID merchantId);
}
