package com.userservice.models;

import com.userservice.models.enums.DietPreference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diet_preferences")
@Data
@NoArgsConstructor

public class DietPreferenceModel {

    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private DietPreference id;

    @Column(name = "description")
    private String description;

}
