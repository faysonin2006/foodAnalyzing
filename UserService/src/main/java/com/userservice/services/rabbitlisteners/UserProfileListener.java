package com.userservice.services.rabbitlisteners;

import com.userservice.dtos.CreateProfileRequest;
import com.userservice.services.UserService;
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
