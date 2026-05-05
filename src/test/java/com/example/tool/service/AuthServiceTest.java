package com.example.tool.service;

import com.example.tool.config.JwtUtil;
import com.example.tool.dto.AuthResponse;
import com.example.tool.dto.LoginRequest;
import com.example.tool.dto.RegisterRequest;
import com.example.tool.entity.User;
import com.example.tool.exception.InvalidDataException;
import com.example.tool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("encoded-password");
        user.setRole("ROLE_VIEWER");
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register user and return token")
        void register_success() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("alice");
            request.setPassword("secret123");

            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtUtil.generateToken("alice", "ROLE_VIEWER")).thenReturn("mock-token");

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("mock-token", response.getToken());
            assertEquals("alice", response.getUsername());
            assertEquals("ROLE_VIEWER", response.getRole());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw InvalidDataException when username already exists")
        void register_duplicateUsername_throwsException() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("alice");
            request.setPassword("secret123");

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            InvalidDataException ex = assertThrows(InvalidDataException.class,
                    () -> authService.register(request));

            assertEquals("Username already exists", ex.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should login and return token")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("alice");
            request.setPassword("secret123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken("alice", "ROLE_VIEWER")).thenReturn("mock-token");

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("mock-token", response.getToken());
            assertEquals("alice", response.getUsername());
        }

        @Test
        @DisplayName("should throw InvalidDataException on bad credentials")
        void login_badCredentials_throwsException() {
            LoginRequest request = new LoginRequest();
            request.setUsername("alice");
            request.setPassword("wrong");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            InvalidDataException ex = assertThrows(InvalidDataException.class,
                    () -> authService.login(request));

            assertEquals("Invalid username or password", ex.getMessage());
        }
    }
}
