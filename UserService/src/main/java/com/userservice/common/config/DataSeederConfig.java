package com.userservice.common.config;


import com.userservice.profile.model.AllergyModel;
import com.userservice.profile.model.DietPreferenceModel;
import com.userservice.profile.model.HealthConditionModel;
import com.userservice.profile.model.enums.Allergy;
import com.userservice.profile.model.enums.DietPreference;
import com.userservice.profile.model.enums.HealthCondition;
import com.userservice.profile.repository.UserAllergyRepository;
import com.userservice.profile.repository.UserDietRepository;
import com.userservice.profile.repository.UserHealthConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataSeederConfig {

    private final UserAllergyRepository userAllergyRepository;
    private final UserDietRepository userDietRepository;
    private final UserHealthConditionRepository userHealthConditionRepository;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            for (Allergy allergy : Allergy.values()) {
                AllergyModel model = userAllergyRepository.findById(allergy).orElseGet(AllergyModel::new);
                model.setId(allergy);
                model.setDescription(allergy.getDescription());
                userAllergyRepository.save(model);
            }

            for (DietPreference diet : DietPreference.values()) {
                DietPreferenceModel model = userDietRepository.findById(diet).orElseGet(DietPreferenceModel::new);
                model.setId(diet);
                model.setDescription(diet.getDescription());
                userDietRepository.save(model);
            }

            for (HealthCondition health : HealthCondition.values()) {
                HealthConditionModel model = userHealthConditionRepository.findById(health).orElseGet(HealthConditionModel::new);
                model.setId(health);
                model.setDescription(health.getDescription());
                userHealthConditionRepository.save(model);
            }
        };
    }
}
