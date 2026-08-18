package com.example.artwork_3d_frontend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.artwork_3d_frontend.dto.LoginDTO;
import com.example.artwork_3d_frontend.dto.RegisterDTO;
import com.example.artwork_3d_frontend.dto.UserUpdateDTO;
import com.example.artwork_3d_frontend.entity.User;
import com.example.artwork_3d_frontend.vo.LoginVO;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    boolean register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    User getCurrentProfile(Long userId);

    boolean updateProfile(Long userId, UserUpdateDTO dto);
}