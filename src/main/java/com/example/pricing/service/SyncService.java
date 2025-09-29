package com.example.pricing.service;

import com.example.pricing.model.PriceConfirmation;
import com.example.pricing.repository.PriceConfirmationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SyncService {
    private final Logger log = LoggerFactory.getLogger(SyncService.class);
    private final PriceConfirmationRepository repo;
    private final RestTemplate restTemplate;

    @Value("${hotel.api.baseUrl:http://localhost:9000}")
    private String baseUrl;

    @Value("${hotel.api.apiKey:dev-key}")
    private String apiKey;

    public SyncService(PriceConfirmationRepository repo) {
        this.repo = repo;
        this.restTemplate = new RestTemplate();
    }

    // run every day at 02:00
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void pushConfirmedPrices() {
        List<PriceConfirmation> pending = repo.findBySyncedFalse();
        log.info("Found {} confirmations to push", pending.size());
        for (PriceConfirmation pc : pending) {
            try {
                boolean ok = pushWithRetry(pc, 3);
                if (ok) {
                    pc.setSynced(true);
                    repo.save(pc);
                }
                log.info("Synced confirmation id={} productId={}", pc.getId(), pc.getProductId());
            } catch (Exception ex) {
                log.error("Failed to sync confirmation id={} productId={}: {}", pc.getId(), pc.getProductId(), ex.getMessage());
            }
        }
    }

    private boolean pushWithRetry(PriceConfirmation pc, int maxAttempts) {
        int attempt = 0;
        long backoffMs = 500L;
        while (attempt < maxAttempts) {
            attempt++;
            try {
                boolean ok = callHotelApi(pc);
                if (ok) return true;
                Thread.sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, 8000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception ex) {
                log.warn("Attempt {} failed for confirmation id={}: {}", attempt, pc.getId(), ex.getMessage());
            }
        }
        return false;
    }

    private boolean callHotelApi(PriceConfirmation pc) {
        String url = baseUrl + "/prices/confirm";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            java.util.Map<String, Object> body = java.util.Map.of(
                    "productId", pc.getProductId(),
                    "action", pc.getAction(),
                    "price", pc.getConfirmedValue(),
                    "currency", pc.getCurrency(),
                    "userId", pc.getUserId(),
                    "confirmedAt", pc.getConfirmedAt().toString()
            );
            HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.error("callHotelApi failed for confirmation id={}: {}", pc.getId(), ex.getMessage());
            return false;
        }
    }
}
