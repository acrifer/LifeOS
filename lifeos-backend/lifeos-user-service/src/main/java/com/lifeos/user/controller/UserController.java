package com.lifeos.user.controller;

import com.lifeos.common.response.Result;
import com.lifeos.user.domain.dto.LoginDTO;
import com.lifeos.user.domain.dto.RegisterDTO;
import com.lifeos.user.domain.dto.UserPasswordUpdateDTO;
import com.lifeos.user.domain.dto.UserProfileUpdateDTO;
import com.lifeos.user.domain.entity.User;
import com.lifeos.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "注册、登录、账户资料和密码管理")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "注册用户", description = "创建新的 LifeOS 用户账号。")
    public Result<Void> register(@RequestBody RegisterDTO registerDTO) {
        try {
            userService.register(registerDTO);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录后返回 JWT token，用于后续通过网关访问业务接口。")
    public Result<String> login(@RequestBody LoginDTO loginDTO) {
        try {
            String token = userService.login(loginDTO);
            return Result.success(token);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的基础资料。")
    public Result<User> getUserInfo(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        try {
            User user = userService.getCurrentUserInfo(userId);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "更新用户资料", description = "更新用户名、邮箱等账户资料，并返回新的 token。")
    public Result<String> updateProfile(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody UserProfileUpdateDTO updateDTO) {
        try {
            return Result.success(userService.updateProfile(userId, updateDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "校验旧密码后更新账户密码，并返回新的 token。")
    public Result<String> updatePassword(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody UserPasswordUpdateDTO updateDTO) {
        try {
            return Result.success(userService.updatePassword(userId, updateDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "注销当前 token，使登录态失效。")
    public Result<Void> logout(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        try {
            userService.logout(userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
