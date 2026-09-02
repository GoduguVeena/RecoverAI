package com.recoverai.backend.agent;

import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.dto.AgentRequestContext;
import com.recoverai.backend.dto.AgentRecommendationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component("fakeAgentModelClient")
public class FakeAgentModelClient implements AgentModelClient {

    @Override
    public AgentRecommendationResponse generateRecommendation(AgentRequestContext context) {
        BigDecimal probability = context.getRecoveryProbability() != null ? context.getRecoveryProbability() : new BigDecimal("0.7500");
        String modelVersion = context.getModelVersion() != null ? context.getModelVersion() : "recovery-logistic-v1";
        String failureCode = context.getFailureCode() != null ? context.getFailureCode() : "UNKNOWN_FAILURE";

        RecoveryActionType selectedAction;
        String diagnosis;
        String reasoning;

        if (probability.compareTo(new BigDecimal("0.7000")) >= 0) {
            selectedAction = RecoveryActionType.RETRY;
            diagnosis = "Transient failure (" + failureCode + ") with high recovery probability (" + probability + ").";
            reasoning = "High predicted recovery probability (" + probability + ") indicates high probability of success upon immediate automated RETRY.";
        } else if (probability.compareTo(new BigDecimal("0.4000")) >= 0) {
            selectedAction = RecoveryActionType.PAYMENT_LINK;
            diagnosis = "Moderate recovery probability (" + probability + ") for failure code " + failureCode + ".";
            reasoning = "Moderate recovery probability (" + probability + "); dispatching personalized PAYMENT_LINK to customer for alternate payment method.";
        } else {
            selectedAction = RecoveryActionType.NOTIFICATION;
            diagnosis = "Low recovery probability (" + probability + ") or high-risk failure (" + failureCode + ").";
            reasoning = "Low recovery probability (" + probability + "); sending NOTIFICATION to customer to update billing details.";
        }

        return new AgentRecommendationResponse(
                modelVersion,
                probability,
                diagnosis,
                List.of(RecoveryActionType.RETRY, RecoveryActionType.PAYMENT_LINK, RecoveryActionType.NOTIFICATION),
                selectedAction,
                reasoning
        );
    }
}
