package com.recoverai.backend.agent;

import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.dto.AgentRecommendationResponse;
import com.recoverai.backend.exception.AgentException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class AgentOutputValidator {

    private static final Set<RecoveryActionType> SUPPORTED_ACTIONS = Set.of(
            RecoveryActionType.RETRY,
            RecoveryActionType.PAYMENT_LINK,
            RecoveryActionType.NOTIFICATION
    );

    public void validate(AgentRecommendationResponse response) {
        if (response == null) {
            throw new AgentException("Agent recommendation output is null.");
        }

        if (response.getSelectedAction() == null) {
            throw new AgentException("Agent recommendation selectedAction is missing.");
        }

        if (!SUPPORTED_ACTIONS.contains(response.getSelectedAction())) {
            throw new AgentException("Agent selected unsupported recovery action: " + response.getSelectedAction());
        }

        if (response.getCandidateActions() == null || response.getCandidateActions().isEmpty()) {
            throw new AgentException("Agent candidateActions list is missing or empty.");
        }

        for (RecoveryActionType candidate : response.getCandidateActions()) {
            if (candidate == null || !SUPPORTED_ACTIONS.contains(candidate)) {
                throw new AgentException("Agent included unsupported candidate action: " + candidate);
            }
        }

        BigDecimal prob = response.getRecoveryProbability();
        if (prob == null || prob.compareTo(BigDecimal.ZERO) < 0 || prob.compareTo(BigDecimal.ONE) > 0) {
            throw new AgentException("Agent recovery probability is missing or out of range [0.0, 1.0]: " + prob);
        }

        if (response.getDiagnosis() == null || response.getDiagnosis().trim().isEmpty()) {
            throw new AgentException("Agent diagnosis is missing or empty.");
        }

        if (response.getReasoningSummary() == null || response.getReasoningSummary().trim().isEmpty()) {
            throw new AgentException("Agent reasoning summary is missing or empty.");
        }

        if (response.getReasoningSummary().length() > 2000) {
            throw new AgentException("Agent reasoning summary exceeds maximum allowed length (2000 chars).");
        }
    }
}
