package com.example.tool.repository;

import com.example.tool.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded-password");
        user.setRole("ROLE_ADMIN");
        userRepository.save(user);
    }

    @Test
    @DisplayName("findByUsername should return user when exists")
    void findByUsername_exists_returnsUser() {
        Optional<User> result = userRepository.findByUsername("alice");
        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        assertEquals("ROLE_ADMIN", result.get().getRole());
    }

    @Test
    @DisplayName("findByUsername should return empty when user does not exist")
    void findByUsername_notFound_returnsEmpty() {
        assertTrue(userRepository.findByUsername("unknown").isEmpty());
    }

    @Test
    @DisplayName("should persist user and assign generated id")
    void save_persistsUserWithId() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("secret");
        user.setRole("ROLE_VIEWER");

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("bob", saved.getUsername());
    }
}
