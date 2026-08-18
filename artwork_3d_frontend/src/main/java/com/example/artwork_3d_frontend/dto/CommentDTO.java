package com.example.artwork_3d_frontend.dto;

import lombok.Data;

@Data
public class CommentDTO {
    private Long artworkId;
    private String content;
}