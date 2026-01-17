package com.aiimageservice.dtos.profiles;

import com.aiimageservice.dtos.profiles.enums.HealthCondition;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "health_conditions")
@Data
public class HealthConditionModel {

    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private HealthCondition id;

    @Column(name = "description")
    private String description;
}
