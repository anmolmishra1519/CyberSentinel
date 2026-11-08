package com.cybersentinel.client;

import com.cybersentinel.dto.PredictionRequest;
import com.cybersentinel.dto.PredictionResponse;
import com.cybersentinel.service.MLServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * The ONLY class in this backend that talks to the FastAPI ML service.
 * Every other component (services, controllers) goes through this client.
 */
@Component
public class MLClient {

    private final WebClient webClient;
    private final int timeoutSeconds;

    public MLClient(WebClient mlServiceWebClient,
                     @Value("${ml.service.timeout-seconds}") int timeoutSeconds) {
        this.webClient = mlServiceWebClient;
        this.timeoutSeconds = timeoutSeconds;
    }

    public PredictionResponse predict(PredictionRequest request) {
        try {
            return webClient.post()
                    .uri("/predict")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PredictionResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (Exception e) {
            throw new MLServiceUnavailableException(
                    "The ML prediction service is unavailable or timed out. " +
                    "Make sure the FastAPI service is running (uvicorn main:app).", e);
        }
    }

    public boolean isHealthy() {
        try {
            Boolean modelLoaded = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .timeout(Duration.ofSeconds(3))
                    .map(m -> Boolean.TRUE.equals(m.get("modelLoaded")))
                    .onErrorReturn(false)
                    .block();
            return Boolean.TRUE.equals(modelLoaded);
        } catch (Exception e) {
            return false;
        }
    }
}
