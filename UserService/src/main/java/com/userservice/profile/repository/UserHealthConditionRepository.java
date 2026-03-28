package com.userservice.profile.repository;

import com.userservice.profile.model.HealthConditionModel;
import com.userservice.profile.model.enums.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHealthConditionRepository extends JpaRepository<HealthConditionModel, HealthCondition> {
}
