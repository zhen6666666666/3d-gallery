package com.example.artwork_3d_frontend.controller;

import com.example.artwork_3d_frontend.dto.UserUpdateDTO;
import com.example.artwork_3d_frontend.entity.User;
import com.example.artwork_3d_frontend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的个人资料
     */
    @GetMapping("/profile")
    public User getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.getCurrentProfile(userId);
    }

    /**
     * 更新当前登录用户的个人资料（昵称、头像、邮箱等）
     */
    @PutMapping("/profile")
    public String updateProfile(@RequestBody UserUpdateDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean success = userService.updateProfile(userId, dto);
        return success ? "个人资料更新成功" : "更新失败";
    }
}