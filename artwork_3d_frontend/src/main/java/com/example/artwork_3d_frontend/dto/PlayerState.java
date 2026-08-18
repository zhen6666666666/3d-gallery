package com.example.artwork_3d_frontend.dto;

import lombok.Data;

@Data
public class PlayerState {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Double x = 0.0;
    private Double y = 0.0;
    private Double z = 0.0;
    private Double rotationY = 0.0;
}