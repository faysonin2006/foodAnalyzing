package com.userservice.pantry.service;

import com.userservice.common.constants.AppMessages;
import com.userservice.common.exceptions.BadRequestException;
import com.userservice.common.exceptions.ProductLookupNotFoundException;
import com.userservice.common.exceptions.ProfileNotFoundException;
import com.userservice.common.security.SecurityUtils;
import com.userservice.pantry.dto.PantryBarcodeLookupResponse;
import com.userservice.pantry.model.PantryBarcodeCache;
import com.userservice.pantry.model.enums.PantryUnit;
import com.userservice.pantry.service.openfoodfacts.dto.OpenFoodFactsProductDto;
import com.userservice.pantry.service.openfoodfacts.dto.OpenFoodFactsProductLookupResponse;
import com.userservice.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PantryBarcodeLookupService {

    private static final String USER_BARCODE_CACHE_SOURCE = "USER_BARCODE_CACHE";
    private static final String OPEN_FOOD_FACTS_SOURCE = "OPEN_FOOD_FACTS";

    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{8,14}$");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*([\\p{L}]+)");
    private static final Pattern AMBIGUOUS_NUMERIC_DATE_PATTERN = Pattern.compile("^(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})$");
    private static final List<DateTimeFormatter> EXPIRATION_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    private final OpenFoodFactsProductClient openFoodFactsProductClient;
    private final PantryBarcodeCacheService pantryBarcodeCacheService;
    private final UserProfileRepository userProfileRepository;

    public PantryBarcodeLookupResponse lookupByBarcode(String barcode) {
        String normalizedBarcode = normalizeBarcode(barcode);
        UUID userId = resolveCurrentUserId();

        PantryBarcodeLookupResponse localLookup = pantryBarcodeCacheService
                .findByUserIdAndBarcode(userId, normalizedBarcode)
                .map(this::mapCacheToLookupResponse)
                .orElse(null);

        PantryBarcodeLookupResponse externalLookup = needsExternalEnrichment(localLookup)
                ? lookupOpenFoodFacts(normalizedBarcode)
                : null;

        if (localLookup != null && externalLookup != null) {
            return mergeLookups(localLookup, externalLookup);
        }
        if (localLookup != null) {
            return localLookup;
        }
        if (externalLookup != null) {
            return externalLookup;
        }

        throw new ProductLookupNotFoundException(AppMessages.PRODUCT_NOT_FOUND_BY_BARCODE);
    }

    private UUID resolveCurrentUserId() {
        return resolveUserIdByEmail(SecurityUtils.getCurrentUsername());
    }

    private UUID resolveUserIdByEmail(String email) {
        return userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ProfileNotFoundException(AppMessages.PROFILE_NOT_FOUND))
                .getId();
    }

    private PantryBarcodeLookupResponse mapCacheToLookupResponse(PantryBarcodeCache cache) {
        Map<String, String> fieldSources = new LinkedHashMap<>();
        putFieldSourceIfPresent(fieldSources, "name", cache.getName(), USER_BARCODE_CACHE_SOURCE);
        putFieldSourceIfPresent(fieldSources, "brand", cache.getBrand(), USER_BARCODE_CACHE_SOURCE);
        putFieldSourceIfPresent(fieldSources, "category", cache.getCategory(), USER_BARCODE_CACHE_SOURCE);
        putFieldSourceIfPresent(fieldSources, "imageUrl", cache.getImageUrl(), USER_BARCODE_CACHE_SOURCE);
        if (cache.getQuantity() != null) {
            fieldSources.put("suggestedQuantity", USER_BARCODE_CACHE_SOURCE);
        }
        if (cache.getUnit() != null) {
            fieldSources.put("suggestedUnit", USER_BARCODE_CACHE_SOURCE);
        }
        putFieldSourceIfPresent(fieldSources, "rawQuantity", cache.getRawQuantity(), USER_BARCODE_CACHE_SOURCE);

        return PantryBarcodeLookupResponse.builder()
                .barcode(blankToNull(cache.getBarcode()))
                .name(blankToNull(cache.getName()))
                .brand(blankToNull(cache.getBrand()))
                .category(blankToNull(cache.getCategory()))
                .imageUrl(blankToNull(cache.getImageUrl()))
                .suggestedQuantity(cache.getQuantity())
                .suggestedUnit(cache.getUnit())
                .rawQuantity(blankToNull(cache.getRawQuantity()))
                .source(USER_BARCODE_CACHE_SOURCE)
                .fieldSources(fieldSources)
                .build();
    }

    private PantryBarcodeLookupResponse lookupOpenFoodFacts(String normalizedBarcode) {
        OpenFoodFactsProductLookupResponse lookupResponse = openFoodFactsProductClient.lookupProductByBarcode(normalizedBarcode);
        OpenFoodFactsProductDto product = lookupResponse == null ? null : lookupResponse.getProduct();

        if (lookupResponse == null
                || lookupResponse.getStatus() == null
                || lookupResponse.getStatus() != 1
                || product == null
                || isBlank(product.getProductName())) {
            return null;
        }

        QuantitySuggestion quantitySuggestion = resolveQuantitySuggestion(
                product.getProductQuantity(),
                product.getProductQuantityUnit(),
                product.getQuantity()
        );

        Map<String, String> fieldSources = new LinkedHashMap<>();
        putFieldSourceIfPresent(fieldSources, "name", product.getProductName(), OPEN_FOOD_FACTS_SOURCE);
        putFieldSourceIfPresent(fieldSources, "brand", product.getBrands(), OPEN_FOOD_FACTS_SOURCE);
        putFieldSourceIfPresent(fieldSources, "category", resolveCategory(product), OPEN_FOOD_FACTS_SOURCE);
        putFieldSourceIfPresent(fieldSources, "imageUrl", product.getImageFrontUrl(), OPEN_FOOD_FACTS_SOURCE);
        if (quantitySuggestion.quantity() != null) {
            fieldSources.put("suggestedQuantity", OPEN_FOOD_FACTS_SOURCE);
        }
        if (quantitySuggestion.unit() != null) {
            fieldSources.put("suggestedUnit", OPEN_FOOD_FACTS_SOURCE);
        }
        putFieldSourceIfPresent(fieldSources, "rawQuantity", product.getQuantity(), OPEN_FOOD_FACTS_SOURCE);
        if (resolveExpirationDate(product.getExpirationDate()) != null) {
            fieldSources.put("expiresAt", OPEN_FOOD_FACTS_SOURCE);
        }

        return PantryBarcodeLookupResponse.builder()
                .barcode(firstNonBlank(product.getCode(), lookupResponse.getCode(), normalizedBarcode))
                .name(product.getProductName().trim())
                .brand(blankToNull(product.getBrands()))
                .category(resolveCategory(product))
                .imageUrl(blankToNull(product.getImageFrontUrl()))
                .suggestedQuantity(quantitySuggestion.quantity())
                .suggestedUnit(quantitySuggestion.unit())
                .rawQuantity(blankToNull(product.getQuantity()))
                .expiresAt(resolveExpirationDate(product.getExpirationDate()))
                .source(OPEN_FOOD_FACTS_SOURCE)
                .fieldSources(fieldSources)
                .build();
    }

    private PantryBarcodeLookupResponse mergeLookups(
            PantryBarcodeLookupResponse primary,
            PantryBarcodeLookupResponse fallback
    ) {
        Map<String, String> mergedFieldSources = new LinkedHashMap<>();
        mergeFieldSource(mergedFieldSources, "name", primary.getName(), primary, fallback.getName(), fallback);
        mergeFieldSource(mergedFieldSources, "brand", primary.getBrand(), primary, fallback.getBrand(), fallback);
        mergeFieldSource(mergedFieldSources, "category", primary.getCategory(), primary, fallback.getCategory(), fallback);
        mergeFieldSource(mergedFieldSources, "imageUrl", primary.getImageUrl(), primary, fallback.getImageUrl(), fallback);
        mergeFieldSource(mergedFieldSources, "suggestedQuantity", primary.getSuggestedQuantity(), primary, fallback.getSuggestedQuantity(), fallback);
        mergeFieldSource(mergedFieldSources, "suggestedUnit", primary.getSuggestedUnit(), primary, fallback.getSuggestedUnit(), fallback);
        mergeFieldSource(mergedFieldSources, "rawQuantity", primary.getRawQuantity(), primary, fallback.getRawQuantity(), fallback);
        mergeFieldSource(mergedFieldSources, "expiresAt", primary.getExpiresAt(), primary, fallback.getExpiresAt(), fallback);

        return PantryBarcodeLookupResponse.builder()
                .barcode(firstNonBlank(primary.getBarcode(), fallback.getBarcode()))
                .name(firstNonBlank(primary.getName(), fallback.getName()))
                .brand(firstNonBlank(primary.getBrand(), fallback.getBrand()))
                .category(firstNonBlank(primary.getCategory(), fallback.getCategory()))
                .imageUrl(firstNonBlank(primary.getImageUrl(), fallback.getImageUrl()))
                .suggestedQuantity(primary.getSuggestedQuantity() != null ? primary.getSuggestedQuantity() : fallback.getSuggestedQuantity())
                .suggestedUnit(primary.getSuggestedUnit() != null ? primary.getSuggestedUnit() : fallback.getSuggestedUnit())
                .rawQuantity(firstNonBlank(primary.getRawQuantity(), fallback.getRawQuantity()))
                .expiresAt(primary.getExpiresAt() != null ? primary.getExpiresAt() : fallback.getExpiresAt())
                .source(resolveLookupSource(primary.getSource(), fallback.getSource()))
                .fieldSources(mergedFieldSources)
                .build();
    }

    private void mergeFieldSource(
            Map<String, String> target,
            String field,
            Object primaryValue,
            PantryBarcodeLookupResponse primary,
            Object fallbackValue,
            PantryBarcodeLookupResponse fallback
    ) {
        if (hasValue(primaryValue)) {
            target.put(field, resolveFieldSource(primary, field));
            return;
        }
        if (hasValue(fallbackValue)) {
            target.put(field, resolveFieldSource(fallback, field));
        }
    }

    private String resolveFieldSource(PantryBarcodeLookupResponse response, String field) {
        if (response.getFieldSources() != null && response.getFieldSources().containsKey(field)) {
            return response.getFieldSources().get(field);
        }
        return firstNonBlank(response.getSource(), OPEN_FOOD_FACTS_SOURCE);
    }

    private boolean needsExternalEnrichment(PantryBarcodeLookupResponse lookup) {
        if (lookup == null) {
            return true;
        }
        return isBlank(lookup.getName())
                || isBlank(lookup.getCategory())
                || lookup.getSuggestedQuantity() == null
                || lookup.getSuggestedUnit() == null
                || isBlank(lookup.getImageUrl());
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.trim().isEmpty();
        }
        return true;
    }

    private void putFieldSourceIfPresent(Map<String, String> fieldSources, String field, String value, String source) {
        if (!isBlank(value)) {
            fieldSources.put(field, source);
        }
    }

    private String resolveLookupSource(String primarySource, String fallbackSource) {
        if (!isBlank(primarySource) && !isBlank(fallbackSource) && !Objects.equals(primarySource, fallbackSource)) {
            return primarySource + "+" + fallbackSource;
        }
        return firstNonBlank(primarySource, fallbackSource);
    }

    private String normalizeBarcode(String barcode) {
        String normalized = barcode == null ? "" : barcode.trim();
        if (!BARCODE_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(AppMessages.INVALID_BARCODE);
        }
        return normalized;
    }

    private String resolveCategory(OpenFoodFactsProductDto product) {
        if (!isBlank(product.getCategories())) {
            String[] rawCategories = product.getCategories().split(",");
            if (rawCategories.length > 0 && !rawCategories[0].isBlank()) {
                return rawCategories[0].trim();
            }
        }
        if (product.getCategoriesTags() != null) {
            for (String tag : product.getCategoriesTags()) {
                if (tag != null && tag.startsWith("en:")) {
                    return humanizeCategory(tag.substring(3));
                }
            }
        }
        return "Other";
    }

    private String humanizeCategory(String value) {
        String[] words = value.replace('-', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(word.substring(1));
        }
        return builder.isEmpty() ? "Other" : builder.toString();
    }

    private LocalDate resolveExpirationDate(String rawValue) {
        if (isBlank(rawValue)) {
            return null;
        }

        String normalized = rawValue.trim();
        for (DateTimeFormatter formatter : EXPIRATION_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported date format.
            }
        }

        Matcher matcher = AMBIGUOUS_NUMERIC_DATE_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            int first = Integer.parseInt(matcher.group(1));
            int second = Integer.parseInt(matcher.group(2));
            int year = Integer.parseInt(matcher.group(3));

            if (first > 12 && second <= 12) {
                return safeDate(year, second, first);
            }
            if (second > 12 && first <= 12) {
                return safeDate(year, first, second);
            }
        }

        return null;
    }

    private LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private QuantitySuggestion resolveQuantitySuggestion(BigDecimal productQuantity, String productQuantityUnit, String rawQuantity) {
        if (productQuantity != null && productQuantity.compareTo(BigDecimal.ZERO) > 0) {
            QuantitySuggestion direct = mapQuantity(productQuantity, productQuantityUnit);
            if (direct != null) {
                return direct;
            }
        }

        if (!isBlank(rawQuantity)) {
            Matcher matcher = QUANTITY_PATTERN.matcher(rawQuantity.toLowerCase(Locale.ROOT));
            if (matcher.find()) {
                BigDecimal amount = new BigDecimal(matcher.group(1).replace(',', '.'));
                QuantitySuggestion parsed = mapQuantity(amount, matcher.group(2));
                if (parsed != null) {
                    return parsed;
                }
            }
        }

        return new QuantitySuggestion(null, null);
    }

    private QuantitySuggestion mapQuantity(BigDecimal amount, String rawUnit) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || isBlank(rawUnit)) {
            return null;
        }

        String normalizedUnit = rawUnit.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedUnit) {
            case "g", "gram", "grams" -> new QuantitySuggestion(scale(amount), PantryUnit.GRAM);
            case "kg", "kilogram", "kilograms" -> new QuantitySuggestion(scale(amount), PantryUnit.KILOGRAM);
            case "ml", "milliliter", "milliliters" -> new QuantitySuggestion(scale(amount), PantryUnit.MILLILITER);
            case "cl", "centiliter", "centiliters" -> new QuantitySuggestion(scale(amount.multiply(BigDecimal.TEN)), PantryUnit.MILLILITER);
            case "dl", "deciliter", "deciliters" -> new QuantitySuggestion(scale(amount.multiply(BigDecimal.valueOf(100))), PantryUnit.MILLILITER);
            case "l", "liter", "liters", "litre", "litres" -> new QuantitySuggestion(scale(amount), PantryUnit.LITER);
            case "pc", "pcs", "piece", "pieces", "unit", "units" -> new QuantitySuggestion(scale(amount), PantryUnit.PIECE);
            case "pack", "packs" -> new QuantitySuggestion(scale(amount), PantryUnit.PACK);
            case "bottle", "bottles" -> new QuantitySuggestion(scale(amount), PantryUnit.BOTTLE);
            case "can", "cans" -> new QuantitySuggestion(scale(amount), PantryUnit.CAN);
            default -> null;
        };
    }

    private BigDecimal scale(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0
                ? value.setScale(0, RoundingMode.HALF_UP)
                : value.stripTrailingZeros();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record QuantitySuggestion(BigDecimal quantity, PantryUnit unit) {
    }
}
