package com.example.artwork_3d_frontend.vo;

import lombok.Data;

@Data
public class ArtworkInteractionVO {
    private Boolean isLiked;
    private Boolean isFavorited;
    private Integer likeCount;
    private Integer favoriteCount;
}