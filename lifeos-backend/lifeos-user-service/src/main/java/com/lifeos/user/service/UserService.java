package com.lifeos.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lifeos.user.domain.dto.LoginDTO;
import com.lifeos.user.domain.dto.RegisterDTO;
import com.lifeos.user.domain.dto.UserPasswordUpdateDTO;
import com.lifeos.user.domain.dto.UserProfileUpdateDTO;
import com.lifeos.user.domain.entity.User;

public interface UserService extends IService<User> {

    /**
     * User Registration
     */
    void register(RegisterDTO registerDTO);

    /**
     * User Login
     * 
     * @return JWT Token
     */
    String login(LoginDTO loginDTO);

    /**
     * Get Current User Info
     */
    User getCurrentUserInfo(Long userId);

    /**
     * Update basic profile and rotate token
     */
    String updateProfile(Long userId, UserProfileUpdateDTO updateDTO);

    /**
     * Update password and rotate token
     */
    String updatePassword(Long userId, UserPasswordUpdateDTO updateDTO);

    /**
     * Logout current session
     */
    void logout(Long userId);
}
