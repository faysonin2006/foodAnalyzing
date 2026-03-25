package com.userservice.models;

import com.userservice.models.enums.Allergy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "allergies")
@Data
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