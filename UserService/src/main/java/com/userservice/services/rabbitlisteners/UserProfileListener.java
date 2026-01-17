package com.userservice.services.rabbitlisteners;

import com.userservice.dtos.CreateProfileRequest;
import com.userservice.services.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileListener {

    private final UserProfileService userProfileService;

    @RabbitListener(queues = "${spring.rabbitmq.template.default-receive-queue}")
    public void handleUserRegistratiob(CreateProfileRequest request) {
        try {
            userProfileService.createProfile(request);
        }
        catch (Exception e) {
            System.err.println("Ошибка при создании профиля: " + e.getMessage());
        }
    }
}
