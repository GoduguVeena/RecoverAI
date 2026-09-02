package com.recoverai.backend.agent;

import com.recoverai.backend.domain.enums.RecoveryActionType;
import com.recoverai.backend.dto.AgentRecommendationResponse;
import com.recoverai.backend.exception.AgentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentOutputValidatorTest {

    private AgentOutputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AgentOutputValidator();
    }

    private AgentRecommendationResponse createValidResponse() {
        return new AgentRecommendationResponse(
                "recovery-logistic-v1",
                new BigDecimal("0.8500"),
                "Transient timeout error",
                List.of(RecoveryActionType.RETRY, RecoveryActionType.PAYMENT_LINK),
                RecoveryActionType.RETRY,
                "High recovery probability (85%) indicates immediate retry."
        );
    }

    @Test
    @DisplayName("1. Valid agent output parses and validates successfully")
    void testValidAgentOutput() {
        AgentRecommendationResponse response = createValidResponse();
        assertDoesNotThrow(() -> validator.validate(response));
    }

    @Test
    @DisplayName("2. Known selected action accepted")
    void testKnownSelectedActionAccepted() {
        AgentRecommendationResponse response = createValidResponse();
        response.setSelectedAction(RecoveryActionType.PAYMENT_LINK);
        assertDoesNotThrow(() -> validator.validate(response));
    }

    @Test
    @DisplayName("3. Unknown / unsupported selected action rejected")
    void testUnsupportedSelectedActionRejected() {
        AgentRecommendationResponse response = createValidResponse();
        response.setSelectedAction(RecoveryActionType.STOP);
        AgentException ex = assertThrows(AgentException.class, () -> validator.validate(response));
        assertTrue(ex.getMessage().contains("unsupported recovery action"));
    }

    @Test
    @DisplayName("4. Missing required field (selectedAction) rejected")
    void testMissingSelectedActionRejected() {
        AgentRecommendationResponse response = createValidResponse();
        response.setSelectedAction(null);
        AgentException ex = assertThrows(AgentException.class, () -> validator.validate(response));
        assertTrue(ex.getMessage().contains("selectedAction is missing"));
    }

    @Test
    @DisplayName("5. Invalid probability (< 0 or > 1) rejected")
    void testInvalidProbabilityRejected() {
        AgentRecommendationResponse response1 = createValidResponse();
        response1.setRecoveryProbability(new BigDecimal("1.5000"));
        AgentException ex = assertThrows(AgentException.class, () -> validator.validate(response1));
        assertTrue(ex.getMessage().contains("out of range"));

        AgentRecommendationResponse response2 = createValidResponse();
        response2.setRecoveryProbability(new BigDecimal("-0.1000"));
        assertThrows(AgentException.class, () -> validator.validate(response2));
    }

    @Test
    @DisplayName("6. Missing diagnosis or reasoning summary rejected")
    void testMissingDiagnosisOrReasoningRejected() {
        AgentRecommendationResponse response1 = createValidResponse();
        response1.setDiagnosis("");
        assertThrows(AgentException.class, () -> validator.validate(response1));

        AgentRecommendationResponse response2 = createValidResponse();
        response2.setReasoningSummary(null);
        assertThrows(AgentException.class, () -> validator.validate(response2));
    }
}
