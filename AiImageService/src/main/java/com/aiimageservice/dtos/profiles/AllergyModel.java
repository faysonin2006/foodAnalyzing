package com.aiimageservice.dtos.profiles;

import com.aiimageservice.dtos.profiles.enums.Allergy;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "allergies")
@Data
public class AllergyModel {

    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private Allergy id;

    @Column(name = "description")
    private String description;
}
