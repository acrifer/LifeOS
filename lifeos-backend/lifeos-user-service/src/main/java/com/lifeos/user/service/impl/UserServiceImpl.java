package com.lifeos.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.common.utils.JwtUtil;
import com.lifeos.user.domain.dto.LoginDTO;
import com.lifeos.user.domain.dto.RegisterDTO;
import com.lifeos.user.domain.entity.User;
import com.lifeos.user.mapper.UserMapper;
import com.lifeos.user.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
        // For production, password MUST be encrypted (e.g. BCrypt)
        user.setPassword(registerDTO.getPassword());
        user.setEmail(registerDTO.getEmail());

        this.save(user);
    }

    @Override
    public String login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        // Login rate limiting: max 5 attempts per minute per username
        String limitKey = "login:limit:" + username;
        Long attempts = stringRedisTemplate.opsForValue().increment(limitKey);
        if (attempts != null && attempts == 1) {
            stringRedisTemplate.expire(limitKey, 1, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts > 5) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        // Find user
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
                .eq(User::getPassword, loginDTO.getPassword());

        User user = this.getOne(wrapper);
        if (user == null) {
            throw new RuntimeException("Invalid username or password");
        }

        // Clear limit on success
        stringRedisTemplate.delete(limitKey);

        // Generate JWT Token
        String token = JwtUtil.generateToken(user.getId(), username);

        // Store in Redis with Key: token:userId (Valid for 24h)
        stringRedisTemplate.opsForValue().set("token:" + user.getId(), token, 24, TimeUnit.HOURS);

        return token;
    }

    @Override
    public User getCurrentUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user != null) {
            user.setPassword(null); // Do not return password
        }
        return user;
    }
}
