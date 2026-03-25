package com.authservice.services;

import com.authservice.config.exceptionhandlers.exceptions.EmailAlreadyExistsException;
import com.authservice.config.exceptionhandlers.exceptions.InvalidRefreshTokenException;
import com.authservice.constants.AppMessages;
import com.authservice.dtos.UserAuthResponse;
import com.authservice.dtos.UserLoginRequest;
import com.authservice.dtos.UserRegistrationRequest;
import com.authservice.dtos.profie.CreateProfileRequest;
import com.authservice.models.RefreshToken;
import com.authservice.models.UserCredentials;
import com.authservice.models.enums.Role;
import com.authservice.repositories.RefreshTokenRepository;
import com.authservice.repositories.UserRepository;
import com.authservice.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final int MAX_ACTIVE_REFRESH_TOKENS = 5;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RabbitTemplate rabbitTemplate;

    @Value("${refresh.token.expiration}")
    private Long refreshTokenExpirationMs;

    @Value("${spring.rabbitmq.template.exchange}")
    private String exchangeName;

    @Value("${spring.rabbitmq.template.routing-key}")
    private String routingKey;

    public UserAuthResponse register(@Valid UserRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(AppMessages.EMAIL_ALREADY_EXISTS);
        }

        UserCredentials user = UserCredentials.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);
        Long expiresIn = jwtService.getExpirationTime();

        CreateProfileRequest profileRequest = CreateProfileRequest.builder()
                .userId(user.getId())
                .email(request.getEmail())
                .build();
        rabbitTemplate.convertAndSend(exchangeName, routingKey, profileRequest);

        return new UserAuthResponse(accessToken, refreshToken.getToken(), expiresIn);
    }

    public UserAuthResponse login(@Valid UserLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserCredentials user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException(AppMessages.USER_NOT_FOUND));

        removeExpiredTokens(user);
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);
        Long expiresIn = jwtService.getExpirationTime();
        return new UserAuthResponse(accessToken, refreshToken.getToken(), expiresIn);
    }

    public UserAuthResponse refreshToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> new UserAuthResponse(
                        jwtService.generateAccessToken(user),
                        refreshToken,
                        jwtService.getExpirationTime()
                ))
                .orElseThrow(() -> new InvalidRefreshTokenException(AppMessages.REFRESH_TOKEN_REVOKED_OR_NOT_FOUND));
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new InvalidRefreshTokenException(AppMessages.REFRESH_TOKEN_EXPIRED_OR_INVALID);
        }
        return token;
    }

    private void removeExpiredTokens(UserCredentials user) {
        var userTokens = refreshTokenRepository.findAllByUser(user);
        if (userTokens.isEmpty()) {
            return;
        }

        var expiredTokens = userTokens.stream()
                .filter(token -> token.getExpiryDate().isBefore(Instant.now()))
                .toList();
        if (!expiredTokens.isEmpty()) {
            refreshTokenRepository.deleteAll(expiredTokens);
        }

        userTokens = refreshTokenRepository.findAllByUser(user);
        if (userTokens.size() >= MAX_ACTIVE_REFRESH_TOKENS) {
            RefreshToken oldestToken = userTokens.stream()
                    .min(Comparator.comparing(RefreshToken::getExpiryDate))
                    .orElse(null);
            if (oldestToken != null) {
                refreshTokenRepository.delete(oldestToken);
            }
        }
    }

    private RefreshToken createRefreshToken(UserCredentials user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }
}
