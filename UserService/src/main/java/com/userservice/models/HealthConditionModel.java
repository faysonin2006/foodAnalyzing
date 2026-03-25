package com.userservice.models;

import com.userservice.models.enums.HealthCondition;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "health_conditions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthConditionModel {
    @Id
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private HealthCondition id;

    @Column(name = "description")
    private String description;
}