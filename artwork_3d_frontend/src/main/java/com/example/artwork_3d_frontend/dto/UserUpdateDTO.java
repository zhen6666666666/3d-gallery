package com.example.artwork_3d_frontend.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String username;
    private String nickname;
    private String password;
    private String avatar;
    private String email;
}