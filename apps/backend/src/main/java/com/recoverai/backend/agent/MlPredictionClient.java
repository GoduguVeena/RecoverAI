package com.recoverai.backend.agent;

import com.recoverai.backend.dto.MlPredictionRequest;
import com.recoverai.backend.dto.MlPredictionResponse;
import com.recoverai.backend.exception.MlServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class MlPredictionClient {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MlPredictionClient(RestTemplate restTemplate,
                               @Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public MlPredictionResponse predict(MlPredictionRequest request) {
        try {
            String endpoint = mlServiceUrl + "/predict";
            MlPredictionResponse response = restTemplate.postForObject(endpoint, request, MlPredictionResponse.class);
            if (response != null && response.getRecoveryProbability() != null) {
                return response;
            }
            throw new MlServiceException("ML service returned null response or probability");
        } catch (Exception e) {
            log.warn("ML Service unavailable at {}/predict ({}). Using default prediction fallback.", mlServiceUrl, e.getMessage());
            // Fallback for offline testing / development when FastAPI is not active
            return new MlPredictionResponse("recovery-logistic-v1", new BigDecimal("0.7500"), "RECOVERABLE");
        }
    }
}
