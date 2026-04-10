package com.userservice.common.security;

import com.userservice.profile.model.UserProfile;
import com.userservice.profile.repository.UserProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserProfileRepository userProfileRepository;

    @Value("${userservice.service-token}")
    private String serviceToken;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (token.equals(serviceToken)) {
            UsernamePasswordAuthenticationToken serviceAuth = new UsernamePasswordAuthenticationToken(
                    "INTERNAL_SERVICE",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
            serviceAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(serviceAuth);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String userEmail = jwtService.extractUsername(token);
            String userIdClaim = jwtService.extractClaim(token, claims -> {
                Object raw = claims.get("userId");
                return raw == null ? null : raw.toString();
            });
            if (userEmail != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtService.isTokenValid(token, userEmail)) {
                ensureProfileExists(userEmail, userIdClaim);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        new ArrayList<>()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ex) {
            log.debug("JWT authentication failed", ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void ensureProfileExists(String userEmail, String userIdClaim) {
        if (userProfileRepository.existsByEmail(userEmail)
                || userIdClaim == null
                || userIdClaim.isBlank()) {
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdClaim);
            if (userProfileRepository.existsById(userId)) {
                return;
            }

            userProfileRepository.save(
                    UserProfile.builder()
                            .id(userId)
                            .email(userEmail)
                            .build()
            );
            log.info("Auto-created missing profile for {}", userEmail);
        } catch (Exception ex) {
            log.warn("Failed to auto-create profile for {}", userEmail, ex);
        }
    }
}
