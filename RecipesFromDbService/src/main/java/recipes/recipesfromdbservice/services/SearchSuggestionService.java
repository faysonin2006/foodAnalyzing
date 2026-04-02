package recipes.recipesfromdbservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.BadRequestException;
import recipes.recipesfromdbservice.constants.AppMessages;
import recipes.recipesfromdbservice.searchml.SmartSuggestionCandidate;
import recipes.recipesfromdbservice.searchml.SmartSuggestionRankItem;
import recipes.recipesfromdbservice.searchml.SmartSuggestionRankRequest;
import recipes.recipesfromdbservice.searchml.SmartSuggestionRankResponse;
import recipes.recipesfromdbservice.searchml.TensorFlowSearchReranker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchSuggestionService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_CANDIDATES = 32;

    private final TensorFlowSearchReranker tensorFlowSearchReranker;

    public SmartSuggestionRankResponse rerankSuggestions(SmartSuggestionRankRequest request) {
        if (request == null) {
            throw new BadRequestException(AppMessages.REQUEST_MUST_NOT_BE_NULL);
        }

        String query = request.query() == null ? "" : request.query().trim();
        List<SmartSuggestionCandidate> normalizedCandidates = normalizeCandidates(request.candidates());
        int limit = request.limit() == null ? DEFAULT_LIMIT : Math.max(1, Math.min(request.limit(), 20));

        if (normalizedCandidates.isEmpty()) {
            return new SmartSuggestionRankResponse(List.of());
        }

        if (query.isBlank()) {
            return new SmartSuggestionRankResponse(
                    normalizedCandidates.stream()
                            .limit(limit)
                            .map(candidate -> new SmartSuggestionRankItem(candidate.id(), 0.0))
                            .toList()
            );
        }

        List<SmartSuggestionRankItem> ranked = tensorFlowSearchReranker.rankSuggestions(query, normalizedCandidates, limit);
        if (ranked.isEmpty()) {
            ranked = normalizedCandidates.stream()
                    .limit(limit)
                    .map(candidate -> new SmartSuggestionRankItem(candidate.id(), 0.0))
                    .toList();
        }
        return new SmartSuggestionRankResponse(ranked);
    }

    private List<SmartSuggestionCandidate> normalizeCandidates(List<SmartSuggestionCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, SmartSuggestionCandidate> deduped = new LinkedHashMap<>();
        for (SmartSuggestionCandidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            String id = candidate.id() == null ? "" : candidate.id().trim();
            String primaryText = candidate.primaryText() == null ? "" : candidate.primaryText().trim();
            if (id.isEmpty() || primaryText.isEmpty()) {
                continue;
            }

            List<String> terms = new ArrayList<>();
            LinkedHashSet<String> seenTerms = new LinkedHashSet<>();
            if (candidate.searchTerms() != null) {
                for (String term : candidate.searchTerms()) {
                    String normalizedTerm = term == null ? "" : term.trim();
                    if (!normalizedTerm.isEmpty() && seenTerms.add(normalizedTerm)) {
                        terms.add(normalizedTerm);
                    }
                }
            }

            deduped.putIfAbsent(
                    id,
                    new SmartSuggestionCandidate(
                            id,
                            primaryText,
                            trimToNull(candidate.secondaryText()),
                            trimToNull(candidate.category()),
                            trimToNull(candidate.brand()),
                            List.copyOf(terms)
                    )
            );
            if (deduped.size() >= MAX_CANDIDATES) {
                break;
            }
        }
        return List.copyOf(deduped.values());
    }

    private String trimToNull(String value) {
        String text = value == null ? "" : value.trim();
        return text.isEmpty() ? null : text;
    }
}
