package com.recoverai.backend.dto;

import com.recoverai.backend.policy.PolicyEvaluationResult;
import java.util.UUID;

public class RecoveryAnalysisResponse {

    private UUID caseId;
    private AgentDecisionResponse agentDecision;
    private PolicyEvaluationResult policyDecision;

    public RecoveryAnalysisResponse() {
    }

    public RecoveryAnalysisResponse(UUID caseId, AgentDecisionResponse agentDecision, PolicyEvaluationResult policyDecision) {
        this.caseId = caseId;
        this.agentDecision = agentDecision;
        this.policyDecision = policyDecision;
    }

    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }

    public AgentDecisionResponse getAgentDecision() { return agentDecision; }
    public void setAgentDecision(AgentDecisionResponse agentDecision) { this.agentDecision = agentDecision; }

    public PolicyEvaluationResult getPolicyDecision() { return policyDecision; }
    public void setPolicyDecision(PolicyEvaluationResult policyDecision) { this.policyDecision = policyDecision; }
}
