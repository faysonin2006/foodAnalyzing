package com.userservice.models.enums;

import lombok.Getter;

@Getter
public enum Allergy {

    GLUTEN("Gluten (wheat, rye, barley, triticale)"),
    LACTOSE("Lactose (milk, cheese, dairy products)"),
    TREE_NUTS("Tree Nuts (almonds, walnuts, hazelnuts, cashews, etc.)"),
    PEANUTS("Peanuts (groundnuts)"),
    EGGS("Eggs (whites and yolks, mayonnaise, baked goods)"),
    SOY("Soy (soybeans, tofu, soy sauce, edamame)"),
    FISH("Fish (salmon, tuna, cod, etc.)"),
    SHELLFISH("Shellfish (shrimp, crab, lobster, clams, mussels)"),
    MUSTARD("Mustard (seeds, powder, sauces)"),
    SESAME("Sesame (seeds, oil, tahini)");

    private final String description;

    Allergy(String description) {
        this.description = description;
    }

    public String toPromptText() {
        return description;
    }
}
