package com.userservice.profile.model.enums;

import lombok.Getter;

@Getter
public enum DietPreference {

    VEGETARIAN("Vegetarian (no meat, but eggs/dairy allowed)"),

    VEGAN("Vegan (strict plant-based, no animal products)"),

    PESCATARIAN("Pescatarian (fish/seafood allowed, no meat)"),

    KETO("Keto (high fat, low carb)"),

    PALEO("Paleo (whole foods, no processed grains/dairy)"),

    HALAL("Halal (per Islamic dietary laws)"),

    KOSHER("Kosher (per Jewish dietary laws)"),

    GLUTEN_FREE("Gluten-Free (no wheat, barley, rye)"),

    LACTOSE_FREE("Lactose-Free (no dairy lactose)"),

    OMNIVORE("Omnivore (no specific restrictions)");

    private final String description;

    DietPreference(String description) {
        this.description = description;
    }

    public String toHumanText() {
        return description;
    }

}
