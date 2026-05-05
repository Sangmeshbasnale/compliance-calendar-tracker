package com.example.tool.service;

import com.example.tool.config.JwtUtil;
import com.example.tool.dto.AuthResponse;
import com.example.tool.dto.LoginRequest;
import com.example.tool.dto.RegisterRequest;
import com.example.tool.entity.User;
import com.example.tool.exception.InvalidDataException;
import com.example.tool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new InvalidDataException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_VIEWER");
        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            throw new InvalidDataException("Invalid username or password");
        }
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        log.info("User logged in: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }
}
