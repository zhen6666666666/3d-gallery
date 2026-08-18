package com.example.artwork_3d_frontend.controller;

import com.example.artwork_3d_frontend.dto.LoginDTO;
import com.example.artwork_3d_frontend.dto.RegisterDTO;
import com.example.artwork_3d_frontend.service.UserService;
import com.example.artwork_3d_frontend.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO dto) {
        try {
            userService.register(dto);
            Map<String, String> res = new HashMap<>();
            res.put("message", "注册成功！");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {
        try {
            LoginVO vo = userService.login(dto);
            return ResponseEntity.ok(vo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}