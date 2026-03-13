package com.lifeos.user.service.impl;

import com.lifeos.common.utils.JwtUtil;
import com.lifeos.user.domain.dto.UserPasswordUpdateDTO;
import com.lifeos.user.domain.dto.UserProfileUpdateDTO;
import com.lifeos.user.domain.entity.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @BeforeAll
    static void initJwtSettings() {
        System.setProperty("LIFEOS_JWT_SECRET", "lifeos-user-service-test-secret-20260312");
        System.setProperty("LIFEOS_JWT_EXPIRATION_MS", "86400000");
    }

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void updateProfileUpdatesFieldsAndRotatesToken() {
        User user = new User();
        user.setId(5L);
        user.setUsername("old-name");
        user.setEmail("old@example.com");

        AtomicReference<User> updatedUser = new AtomicReference<>();
        doReturn(user).when(userService).getById(5L);
        doReturn(0L).when(userService).count(any());
        doAnswer(invocation -> {
            updatedUser.set(invocation.getArgument(0));
            return true;
        }).when(userService).updateById(any(User.class));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        UserProfileUpdateDTO updateDTO = new UserProfileUpdateDTO();
        updateDTO.setUsername("new-name");
        updateDTO.setEmail("new@example.com");

        String token = userService.updateProfile(5L, updateDTO);

        assertThat(token).isNotBlank();
        assertThat(updatedUser.get()).isNotNull();
        assertThat(updatedUser.get().getUsername()).isEqualTo("new-name");
        assertThat(updatedUser.get().getEmail()).isEqualTo("new@example.com");
        verify(valueOperations).set(eq("token:5"), eq(token), eq(JwtUtil.getExpirationTimeMs()), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void updatePasswordHashesNewPasswordAndRotatesToken() {
        User user = new User();
        user.setId(9L);
        user.setUsername("tester");
        user.setPassword("$2a$legacyhash");

        AtomicReference<User> updatedUser = new AtomicReference<>();
        doReturn(user).when(userService).getById(9L);
        doAnswer(invocation -> {
            updatedUser.set(invocation.getArgument(0));
            return true;
        }).when(userService).updateById(any(User.class));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(passwordEncoder.matches("old-pass", "$2a$legacyhash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass-123")).thenReturn("hashed-new-pass");

        UserPasswordUpdateDTO updateDTO = new UserPasswordUpdateDTO();
        updateDTO.setCurrentPassword("old-pass");
        updateDTO.setNewPassword("new-pass-123");

        String token = userService.updatePassword(9L, updateDTO);

        assertThat(token).isNotBlank();
        assertThat(updatedUser.get()).isNotNull();
        assertThat(updatedUser.get().getPassword()).isEqualTo("hashed-new-pass");
        verify(valueOperations).set(eq("token:9"), eq(token), eq(JwtUtil.getExpirationTimeMs()), eq(TimeUnit.MILLISECONDS));
    }
}
