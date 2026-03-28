package com.userservice.profile.repository;

import com.userservice.profile.model.AllergyModel;
import com.userservice.profile.model.enums.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAllergyRepository extends JpaRepository<AllergyModel, Allergy> {
}
