package com.aiimageservice.services;

import com.aiimageservice.dtos.GeminiNutritionData;
import com.aiimageservice.dtos.profiles.AllergyModel;
import com.aiimageservice.dtos.profiles.DietPreferenceModel;
import com.aiimageservice.dtos.profiles.HealthConditionModel;
import com.aiimageservice.dtos.profiles.UserProfileResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiNutritionData analyzeFood(byte[] imageBytes, UserProfileResponse userProfile) throws Exception {

        String profileContext = buildProfileContext(userProfile);

        String textPrompt = """
                Ты — профессиональный диетолог и нутрициолог.
                Твоя задача:
                1. Определить блюдо на изображении.
                2. Оценить примерное количество калорий, белков, жиров и углеводов на порцию.
                3. Дать персональную рекомендацию с учетом профиля пользователя.

                %s

                Требования к ответу:
                - Если в блюде есть продукты из списка аллергий пользователя, начни extra_info с ПРЕДУПРЕЖДЕНИЯ КРУПНЫМИ БУКВАМИ.
                - В extra_info дай 2–3 предложения: можно ли этому пользователю есть это блюдо с учетом его цели, веса, уровня активности и заболеваний.
                - Никакого Markdown, никаких ```json. Ответ должен быть СТРОГО в формате чистого JSON.

                Шаблон JSON ответа:
                {
                  "dish_name": "Название блюда",
                  "calories": 0,
                  "protein": 0.0,
                  "carbs": 0.0,
                  "fats": 0.0,
                  "extra_info": "Краткая рекомендация..."
                }
                """.formatted(profileContext);

        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageBytes));

        UserMessage userMessage = UserMessage.builder()
                .text(textPrompt)
                .media(media)
                .build();

        ChatResponse response = chatModel.call(new Prompt(userMessage));

        String jsonResponse = response.getResult().getOutput().getText();
        log.info("Gemini Raw: {}", jsonResponse);

        return parseResponse(jsonResponse);
    }

    private String buildProfileContext(UserProfileResponse profile) {
        if (profile == null) {
            return "Профиль пользователя не предоставлен. Дай общие рекомендации по здоровому питанию.";
        }

        StringBuilder sb = new StringBuilder("ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ:\n");

        if (profile.getGender() != null) {
            sb.append("- Пол: ").append(profile.getGender()).append("\n");
        }
        if (profile.getDateOfBirth() != null) {
            sb.append("- Дата рождения: ").append(profile.getDateOfBirth()).append("\n");
        }
        if (profile.getWeight() != null) {
            sb.append("- Вес: ").append(profile.getWeight()).append(" кг\n");
        }
        if (profile.getHeight() != null) {
            sb.append("- Рост: ").append(profile.getHeight()).append(" см\n");
        }
        if (profile.getActivityLevel() != null) {
            sb.append("- Уровень активности: ").append(profile.getActivityLevel()).append("\n");
        }
        if (profile.getGoalType() != null) {
            sb.append("- Цель: ").append(profile.getGoalType()).append("\n");
        }
        if (profile.getTargetCalories() != null) {
            sb.append("- Целевая калорийность в день: ").append(profile.getTargetCalories()).append(" ккал\n");
        }

        if (profile.getDietPreferences() != null && !profile.getDietPreferences().isEmpty()) {
            String diet = profile.getDietPreferences().stream()
                    .map(dietPreferenceModel -> dietPreferenceModel.getId().getDescription())
                    .collect(Collectors.joining(", "));
            sb.append("- Диетические предпочтения: ").append(diet).append("\n");
        }

        if (profile.getAllergies() != null && !profile.getAllergies().isEmpty()) {
            String allergies = profile.getAllergies().stream()
                    .map(allergyModel -> allergyModel.getId().getDescription())
                    .collect(Collectors.joining(", "));
            sb.append("- !!! АЛЛЕРГИИ: ").append(allergies).append("\n");
        }

        if (profile.getHealthConditions() != null && !profile.getHealthConditions().isEmpty()) {
            String conditions = profile.getHealthConditions().stream()
                    .map(healthConditionModel -> healthConditionModel.getId().getDescription())
                    .collect(Collectors.joining(", "));
            sb.append("- Заболевания: ").append(conditions).append("\n");
        }

        return sb.toString();
    }

    private GeminiNutritionData parseResponse(String json) throws Exception {
        String clean = json
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
        return objectMapper.readValue(clean, GeminiNutritionData.class);
    }
}
