package com.recipeservice.dtos.spoonacular;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpoonAnalyzedInstructionDto {

    private String name;

    private List<StepDto> steps = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepDto {
        private Integer number;
        private String step;

        private List<IngredientRefDto> ingredients = new ArrayList<>();
        private List<EquipmentRefDto> equipment = new ArrayList<>();

        private AmountUnitDto length;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IngredientRefDto {
        private Long id;
        private String image;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EquipmentRefDto {
        private Long id;
        private String image;
        private String name;

        private AmountUnitDto temperature;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AmountUnitDto {
        private Double number;
        private String unit;
    }
}
