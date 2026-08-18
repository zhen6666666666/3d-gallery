package com.example.artwork_3d_frontend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.artwork_3d_frontend.entity.Artwork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ArtworkMapper extends BaseMapper<Artwork> {

    // 1. 查询我的发布
    @Select("SELECT * FROM artwork WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Artwork> selectByUserId(Long userId);

    // 2. 查询我的点赞作品
    @Select("SELECT a.* FROM artwork a INNER JOIN artwork_like l ON a.id = l.artwork_id WHERE l.user_id = #{userId} ORDER BY l.create_time DESC")
    List<Artwork> selectLikedArtworksByUserId(Long userId);

    // 3. 查询我的收藏作品
    @Select("SELECT a.* FROM artwork a INNER JOIN artwork_favorite f ON a.id = f.artwork_id WHERE f.user_id = #{userId} ORDER BY f.create_time DESC")
    List<Artwork> selectFavoritedArtworksByUserId(Long userId);
}