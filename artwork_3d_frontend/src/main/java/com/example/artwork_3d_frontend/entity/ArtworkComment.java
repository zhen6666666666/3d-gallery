package com.example.artwork_3d_frontend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("artwork_comment")
public class ArtworkComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long artworkId;
    private Long userId;
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}