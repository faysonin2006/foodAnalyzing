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

    public GeminiNutritionData analyzeFood(byte[] imageBytes, UserProfileResponse userProfile, String questions) throws Exception {

        String profileContext = buildProfileContext(userProfile);

        String questionsBlock = (questions != null && !questions.isBlank())
                ? "\nДОПОЛНИТЕЛЬНЫЕ ВОПРОСЫ ПОЛЬЗОВАТЕЛЯ (ответь на них кратко в extra_info):\n" + questions
                : "";

        String textPrompt = """
            Ты — профессиональный диетолог и нутрициолог.
            Твоя задача:
            1. Определить блюдо на изображении.
            2. Оценить примерное количество калорий, белков, жиров и углеводов на порцию.
            3. Дать персональную рекомендацию с учетом профиля пользователя.
            4. Кратко ответить на вопросы пользователя, если они указаны ниже.

            КОНТЕКСТ ПОЛЬЗОВАТЕЛЯ:
            %s
            %s

            Требования к ответу:
            - Если в блюде есть аллергены пользователя, начни поле extra_info с текста "ВНИМАНИЕ: АЛЛЕРГЕН!".
            - В поле extra_info напиши суммарно 3–5 предложений. Включи туда:
                а) Рекомендацию (можно ли это есть при текущей цели/весе).
                б) Прямые и краткие ответы на вопросы пользователя (если они были).
            - Пиши максимально лаконично, только по существу фото и вопросов.
            - Никакого Markdown, никаких ```json. Ответ СТРОГО в формате чистого JSON.

            Шаблон JSON:
            {
              "dish_name": "Название блюда",
              "calories": 0,
              "protein": 0.0,
              "carbs": 0.0,
              "fats": 0.0,
              "extra_info": "Текст рекомендации и ответы на вопросы..."
            }
            """.formatted(profileContext, questionsBlock);

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
