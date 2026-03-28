package com.userservice.profile.model;

import com.userservice.profile.model.enums.DietPreference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "diet_preferences")
@Getter
@Setter
@NoArgsConstructor
public class DietPreferenceModel {

    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private DietPreference id;

    @Column(name = "description")
    private String description;
}
