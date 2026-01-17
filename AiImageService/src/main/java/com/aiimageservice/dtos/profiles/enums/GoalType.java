package com.aiimageservice.dtos.profiles.enums;

import lombok.Getter;

@Getter
public enum GoalType {

    LOSE_WEIGHT("Lose weight (fat loss)", -0.20),

    MAINTAIN_WEIGHT("Maintain weight", 0.0),

    GAIN_MUSCLE("Gain muscle", 0.15);

    private final String description;

    private final double adjustmentFactor;

    GoalType(String description, double adjustmentFactor) {
        this.description = description;
        this.adjustmentFactor = adjustmentFactor;
    }

    public String toHumanText() {
        return description;
    }
}
