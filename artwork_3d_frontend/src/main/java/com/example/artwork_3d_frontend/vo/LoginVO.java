package com.example.artwork_3d_frontend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}