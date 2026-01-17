package com.aiimageservice.dtos.profiles.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ActivityLevel {

    SEDENTARY(1.20, "Sedentary (little or no exercise)"),
    LIGHTLY_ACTIVE(1.375, "Lightly active (1–3 workouts per week)"),
    MODERATELY_ACTIVE(1.55, "Moderately active (3–5 workouts per week)"),
    VERY_ACTIVE(1.725, "Very active (6–7 workouts per week)"),
    EXTRA_ACTIVE(1.90, "Extra active (physical job and/or intense training)");

    private final double multiplier;
    private final String description;

    ActivityLevel(double multiplier, String description) {
        this.multiplier = multiplier;
        this.description = description;
    }

    public String toHumanText() {
        return description;
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}

