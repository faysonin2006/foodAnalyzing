package recipes.recipesfromdbservice.searchml;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TensorFlowSearchReranker {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(900))
            .build();
    private final AtomicLong circuitOpenUntilMs = new AtomicLong(0);

    @Value("${search.ml.enabled:false}")
    private boolean enabled;

    @Value("${search.ml.base-url:http://recipe-search-ml-service:8107}")
    private String baseUrl;

    @Value("${search.ml.timeout-ms:1500}")
    private long timeoutMs;

    @Value("${search.ml.max-candidates:120}")
    private int maxCandidates;

    @Value("${search.ml.min-score:0.16}")
    private double minScore;

    @Value("${search.ml.suggestion-max-candidates:32}")
    private int suggestionMaxCandidates;

    @Value("${search.ml.failure-cooldown-ms:30000}")
    private long failureCooldownMs;

    public Map<Long, Double> rerank(String query, List<SemanticSearchCandidate> candidates) {
        if (!enabled || query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return Map.of();
        }

        long now = System.currentTimeMillis();
        if (now < circuitOpenUntilMs.get()) {
            return Map.of();
        }

        List<SemanticSearchCandidate> limitedCandidates = candidates.stream()
                .filter(candidate -> candidate.recipeId() != null)
                .limit(Math.max(10, maxCandidates))
                .toList();
        if (limitedCandidates.isEmpty()) {
            return Map.of();
        }

        try {
            SemanticRerankRequest payload = new SemanticRerankRequest(query, limitedCandidates, limitedCandidates.size());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/rerank"))
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 300)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.OK.value()) {
                openCircuit(now, "Unexpected status from search ML service: {}", response.statusCode());
                return Map.of();
            }

            SemanticRerankResponse rerankResponse = objectMapper.readValue(response.body(), SemanticRerankResponse.class);
            if (rerankResponse == null || rerankResponse.scores() == null || rerankResponse.scores().isEmpty()) {
                return Map.of();
            }

            Map<Long, Double> scores = new LinkedHashMap<>();
            for (SemanticSearchScore score : rerankResponse.scores()) {
                if (score == null || score.recipeId() == null || score.score() == null) {
                    continue;
                }
                if (score.score() >= minScore) {
                    scores.put(score.recipeId(), score.score());
                }
            }
            return Map.copyOf(scores);
        } catch (Exception exception) {
            openCircuit(now, "Search ML rerank failed: {}", exception.getMessage());
            return Map.of();
        }
    }

    public List<SmartSuggestionRankItem> rankSuggestions(
            String query,
            List<SmartSuggestionCandidate> candidates,
            Integer limit
    ) {
        if (!enabled || query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        long now = System.currentTimeMillis();
        if (now < circuitOpenUntilMs.get()) {
            return List.of();
        }

        int cappedLimit = limit == null ? 8 : Math.max(1, Math.min(limit, 20));
        List<SmartSuggestionCandidate> limitedCandidates = candidates.stream()
                .filter(candidate -> candidate.id() != null && !candidate.id().isBlank())
                .limit(Math.max(cappedLimit, Math.max(8, suggestionMaxCandidates)))
                .toList();
        if (limitedCandidates.isEmpty()) {
            return List.of();
        }

        try {
            SmartSuggestionRankRequest payload = new SmartSuggestionRankRequest(query, limitedCandidates, cappedLimit);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/suggest"))
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 300)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.OK.value()) {
                openCircuit(now, "Unexpected status from search ML suggestion service: {}", response.statusCode());
                return List.of();
            }

            SmartSuggestionRankResponse suggestionResponse = objectMapper.readValue(response.body(), SmartSuggestionRankResponse.class);
            if (suggestionResponse == null || suggestionResponse.items() == null || suggestionResponse.items().isEmpty()) {
                return List.of();
            }

            return suggestionResponse.items().stream()
                    .filter(item -> item != null && item.id() != null && !item.id().isBlank())
                    .limit(cappedLimit)
                    .toList();
        } catch (Exception exception) {
            openCircuit(now, "Search ML suggestion rerank failed: {}", exception.getMessage());
            return List.of();
        }
    }

    private void openCircuit(long now, String template, Object value) {
        circuitOpenUntilMs.set(now + Math.max(failureCooldownMs, 5_000L));
        log.debug(template, value);
    }
}
