package com.lifeos.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.common.utils.JwtUtil;
import com.lifeos.user.domain.dto.LoginDTO;
import com.lifeos.user.domain.dto.RegisterDTO;
import com.lifeos.user.domain.dto.UserPasswordUpdateDTO;
import com.lifeos.user.domain.dto.UserProfileUpdateDTO;
import com.lifeos.user.domain.entity.User;
import com.lifeos.user.mapper.UserMapper;
import com.lifeos.user.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String TOKEN_KEY_PREFIX = "token:";
    private static final String LOGIN_LIMIT_KEY_PREFIX = "login:limit:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterDTO registerDTO) {
        // Check if username exists
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        long count = this.count(wrapper);
        if (count > 0) {
            throw new RuntimeException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());

        this.save(user);
    }

    @Override
    public String login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        // Login rate limiting: max 5 attempts per minute per username
        String limitKey = LOGIN_LIMIT_KEY_PREFIX + username;
        Long attempts = stringRedisTemplate.opsForValue().increment(limitKey);
        if (attempts != null && attempts == 1) {
            stringRedisTemplate.expire(limitKey, 1, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts > 5) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        // Find user
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);

        User user = this.getOne(wrapper);
        if (user == null || !isPasswordValid(user, loginDTO.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // Clear limit on success
        stringRedisTemplate.delete(limitKey);

        return rotateToken(user);
    }

    @Override
    public User getCurrentUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user != null) {
            user.setPassword(null); // Do not return password
        }
        return user;
    }

    @Override
    public String updateProfile(Long userId, UserProfileUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new RuntimeException("Update request is required");
        }

        User user = getRequiredUser(userId);
        String username = normalizeRequiredText(updateDTO.getUsername(), "Username is required");
        String email = normalizeOptionalText(updateDTO.getEmail());

        validateUsernameAvailable(userId, username);

        user.setUsername(username);
        user.setEmail(email);
        if (!this.updateById(user)) {
            throw new RuntimeException("Failed to update profile");
        }

        return rotateToken(user);
    }

    @Override
    public String updatePassword(Long userId, UserPasswordUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new RuntimeException("Password update request is required");
        }

        String currentPassword = normalizeRequiredText(updateDTO.getCurrentPassword(), "Current password is required");
        String newPassword = normalizeRequiredText(updateDTO.getNewPassword(), "New password is required");
        if (newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        User user = getRequiredUser(userId);
        if (!isPasswordValid(user, currentPassword)) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        if (!this.updateById(user)) {
            throw new RuntimeException("Failed to update password");
        }

        return rotateToken(user);
    }

    @Override
    public void logout(Long userId) {
        stringRedisTemplate.delete(TOKEN_KEY_PREFIX + userId);
    }

    private boolean isPasswordValid(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        if (!storedPassword.equals(rawPassword)) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        this.updateById(user);
        return true;
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private User getRequiredUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    private void validateUsernameAvailable(Long userId, String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username).ne(User::getId, userId);
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("Username already exists");
        }
    }

    private String rotateToken(User user) {
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        stringRedisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + user.getId(),
                token,
                JwtUtil.getExpirationTimeMs(),
                TimeUnit.MILLISECONDS);
        return token;
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
