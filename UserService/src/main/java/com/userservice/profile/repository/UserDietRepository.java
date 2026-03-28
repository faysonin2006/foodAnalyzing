package com.userservice.profile.repository;

import com.userservice.profile.model.DietPreferenceModel;
import com.userservice.profile.model.enums.DietPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDietRepository extends JpaRepository<DietPreferenceModel, DietPreference> {
}
