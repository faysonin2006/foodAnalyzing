package com.userservice.models;

import com.userservice.models.enums.Allergy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "allergies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AllergyModel {

    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private Allergy id;

    @Column(name = "description")
    private String description;
}
