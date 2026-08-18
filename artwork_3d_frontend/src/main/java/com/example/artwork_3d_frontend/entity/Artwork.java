package com.example.artwork_3d_frontend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("artwork")
public class Artwork {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String author;
    private String year;
    private String description;
    private String url;
    private Integer positionIndex;
    private LocalDateTime createTime;
    private Integer likeCount;
    private Integer favoriteCount;
}