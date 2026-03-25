package com.userservice.models;

import com.userservice.models.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private Gender gender;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "activity_level")
    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    @Column(name = "goal_type")
    @Enumerated(EnumType.STRING)
    private GoalType goalType;

    @Column(name = "target_calories_per_day")
    private Integer targetCaloriesPerDay;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_diet_preferences",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "diet_id")
    )
    private List<DietPreferenceModel> dietPreferences;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_allergies",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "allergy_id")
    )
    private List<AllergyModel> allergies;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_health_conditions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "health_condition_id")
    )
    private List<HealthConditionModel> healthConditions;

    @CreationTimestamp
    private LocalDate createdAt;

    @UpdateTimestamp
    private LocalDate updatedAt;
}
