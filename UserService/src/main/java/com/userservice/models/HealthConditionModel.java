package com.userservice.models;

import com.userservice.models.enums.HealthCondition;
import jakarta.persistence.*;

@Entity
@Table(name = "health_conditions")
public class HealthConditionModel {

    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private HealthCondition id;

    @Column(name = "description")
    private String description;
}
