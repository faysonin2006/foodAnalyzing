package com.userservice.models;

import com.userservice.models.enums.Allergy;
import jakarta.persistence.*;

@Entity
@Table(name = "allergies")
public class AllergyModel {

    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private Allergy id;

    @Column(name = "description")
    private String description;
}
