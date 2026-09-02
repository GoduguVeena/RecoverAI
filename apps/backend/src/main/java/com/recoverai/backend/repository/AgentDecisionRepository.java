package com.recoverai.backend.repository;

import com.recoverai.backend.domain.entity.AgentDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentDecisionRepository extends JpaRepository<AgentDecision, UUID> {
    List<AgentDecision> findByRecoveryCaseId(UUID recoveryCaseId);
}
