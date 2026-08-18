package com.example.artwork_3d_frontend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.artwork_3d_frontend.config.JwtUtils;
import com.example.artwork_3d_frontend.dto.LoginDTO;
import com.example.artwork_3d_frontend.dto.RegisterDTO;
import com.example.artwork_3d_frontend.dto.UserUpdateDTO;
import com.example.artwork_3d_frontend.entity.User;
import com.example.artwork_3d_frontend.mapper.UserMapper;
import com.example.artwork_3d_frontend.service.UserService;
import com.example.artwork_3d_frontend.vo.LoginVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtils jwtUtils;

    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean register(RegisterDTO dto) {
        Long count = this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在！");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : "展厅游客");

        return this.save(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户不存在或密码错误！");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    @Override
    public User getCurrentProfile(Long userId) {
        return this.getById(userId);
    }

    @Override
    public boolean updateProfile(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;

        if (StringUtils.hasText(dto.getUsername())) {
            user.setUsername(dto.getUsername());
        }
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        // 当输入新密码时，加密后再存入数据库
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return userMapper.updateById(user) > 0;
    }
}