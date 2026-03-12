package com.lifeos.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lifeos.user.domain.dto.LoginDTO;
import com.lifeos.user.domain.dto.RegisterDTO;
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
}
