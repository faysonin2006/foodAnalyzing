package com.userservice.models;

import com.userservice.models.enums.HealthCondition;
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
@Table(name = "health_conditions")
@Getter
@Setter
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
