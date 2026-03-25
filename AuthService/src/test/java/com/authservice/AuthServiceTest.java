package com.authservice;

import com.authservice.config.exceptionhandlers.exceptions.EmailAlreadyExistsException;
import com.authservice.config.exceptionhandlers.exceptions.InvalidRefreshTokenException;
import com.authservice.dtos.UserAuthResponse;
import com.authservice.dtos.profie.CreateProfileRequest;
import com.authservice.models.RefreshToken;
import com.authservice.models.UserCredentials;
import com.authservice.repositories.RefreshTokenRepository;
import com.authservice.repositories.UserRepository;
import com.authservice.security.JwtService;
import com.authservice.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 3_600_000L);
        ReflectionTestUtils.setField(authService, "exchangeName", "userauth.exchange");
        ReflectionTestUtils.setField(authService, "routingKey", "userauth.tracking");
    }

    @Test
    void registerShouldCreateUserAndPublishProfileEvent() {
        var request = TestDataFactory.registrationRequest();
        var user = TestDataFactory.user();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(UserCredentials.class))).thenReturn(user);
        when(jwtService.generateAccessToken(any(UserCredentials.class))).thenReturn("access-token");
        when(jwtService.getExpirationTime()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken token = invocation.getArgument(0);
                    token.setId(1L);
                    token.setToken("refresh-token");
                    return token;
                });

        UserAuthResponse actual = authService.register(request);

        assertEquals(new UserAuthResponse("access-token", "refresh-token", 900_000L), actual);
        ArgumentCaptor<CreateProfileRequest> profileCaptor = ArgumentCaptor.forClass(CreateProfileRequest.class);
        verify(rabbitTemplate).convertAndSend(eq("userauth.exchange"), eq("userauth.tracking"), profileCaptor.capture());
        assertEquals(TestDataFactory.USER_ID, profileCaptor.getValue().getUserId());
        assertEquals("user@example.com", profileCaptor.getValue().getEmail());
    }

    @Test
    void registerShouldFailWhenEmailAlreadyExists() {
        var request = TestDataFactory.registrationRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(TestDataFactory.user()));

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginShouldAuthenticateAndReturnTokens() {
        var request = TestDataFactory.loginRequest();
        var user = TestDataFactory.user();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByUser(user)).thenReturn(List.of());
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getExpirationTime()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken token = invocation.getArgument(0);
                    token.setToken("refresh-token");
                    return token;
                });

        UserAuthResponse actual = authService.login(request);

        assertEquals(new UserAuthResponse("access-token", "refresh-token", 900_000L), actual);
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void refreshTokenShouldThrowWhenExpired() {
        var user = TestDataFactory.user();
        var token = TestDataFactory.refreshToken(user, Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken("refresh-token"));
        verify(refreshTokenRepository).delete(token);
    }
}
