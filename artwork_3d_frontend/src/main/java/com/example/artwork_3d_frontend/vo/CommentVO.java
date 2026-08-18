package com.example.artwork_3d_frontend.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long artworkId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private LocalDateTime createTime;
}