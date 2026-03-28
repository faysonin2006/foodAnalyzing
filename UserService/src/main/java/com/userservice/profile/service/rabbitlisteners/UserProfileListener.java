package com.userservice.profile.service.rabbitlisteners;

import com.userservice.profile.dto.CreateProfileRequest;
import com.userservice.profile.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileListener {

    private final UserService userService;

    @RabbitListener(queues = "${spring.rabbitmq.template.default-receive-queue}")
    public void handleUserRegistration(CreateProfileRequest request) {

        try {
            userService.createProfile(request);
        }
        catch (Exception e) {
            System.err.println("Ошибка при создании профиля: " + e.getMessage());
        }
    }
}
