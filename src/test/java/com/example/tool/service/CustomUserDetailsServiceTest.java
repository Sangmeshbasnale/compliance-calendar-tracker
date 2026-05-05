package com.example.tool.service;

import com.example.tool.entity.User;
import com.example.tool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("encoded-password");
        user.setRole("ROLE_ADMIN");
    }

    @Test
    @DisplayName("should return UserDetails when user exists")
    void loadUserByUsername_userExists_returnsUserDetails() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("alice");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        assertEquals("encoded-password", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(userRepository).findByUsername("alice");
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("unknown"));

        assertEquals("User not found: unknown", ex.getMessage());
        verify(userRepository).findByUsername("unknown");
    }

    @Test
    @DisplayName("should map role correctly as granted authority")
    void loadUserByUsername_roleViewer_mappedCorrectly() {
        user.setRole("ROLE_VIEWER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("alice");

        assertEquals(1, result.getAuthorities().size());
        assertEquals("ROLE_VIEWER",
                result.getAuthorities().iterator().next().getAuthority());
    }
}
