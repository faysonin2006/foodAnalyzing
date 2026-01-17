package com.userservice.repositories;

import com.userservice.models.HealthConditionModel;
import com.userservice.models.enums.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHealthConditionRepository extends JpaRepository<HealthConditionModel, HealthCondition> {
}
