package com.recoverai.backend.agent;

import com.recoverai.backend.dto.AgentRequestContext;
import com.recoverai.backend.dto.AgentRecommendationResponse;

public interface AgentModelClient {
    AgentRecommendationResponse generateRecommendation(AgentRequestContext context);
}
