package com.userservice.repositories;

import com.userservice.models.DietPreferenceModel;
import com.userservice.models.enums.DietPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDietRepository extends JpaRepository<DietPreferenceModel, DietPreference> {
}
