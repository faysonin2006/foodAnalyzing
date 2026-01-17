package com.userservice.repositories;

import com.userservice.models.AllergyModel;
import com.userservice.models.enums.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAllergyRepository extends JpaRepository<AllergyModel, Allergy> {
}
