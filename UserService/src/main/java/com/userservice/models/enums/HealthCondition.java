package com.userservice.models.enums;

import lombok.Getter;

@Getter
public enum HealthCondition {

    DIABETES_TYPE_1("Type 1 Diabetes (requires strict carb counting and insulin management)"),
    DIABETES_TYPE_2("Type 2 Diabetes (limit simple carbs and sugars)"),
    INSULIN_RESISTANCE("Insulin Resistance (pre-diabetic condition, limit refined carbs)"),
    GASTRITIS("Gastritis (avoid spicy, acidic, fried, and fatty foods)"),
    HYPERTENSION("Hypertension (limit sodium and salty foods)"),
    HIGH_CHOLESTEROL("High Cholesterol (limit saturated fats and trans fats)"),
    PREGNANCY("Pregnancy (increase folate, iron, calcium, avoid raw foods)"),
    GOUT("Gout (limit purines: red meat, seafood, alcohol)"),
    KIDNEY_DISEASE("Kidney Disease (limit protein, potassium, phosphorus, sodium)"),
    CELIAC_DISEASE("Celiac Disease (strict gluten avoidance required)");

    private final String description;

    HealthCondition(String description) {
        this.description = description;
    }

    public String toPromptText() {
        return description;
    }
}

