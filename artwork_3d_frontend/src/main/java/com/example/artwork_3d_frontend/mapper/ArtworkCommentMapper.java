package com.example.artwork_3d_frontend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.artwork_3d_frontend.entity.ArtworkComment;
import com.example.artwork_3d_frontend.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArtworkCommentMapper extends BaseMapper<ArtworkComment> {

    @Select("SELECT c.id, c.artwork_id, c.user_id, c.content, c.create_time, " +
            "u.nickname, u.avatar " +
            "FROM artwork_comment c " +
            "LEFT JOIN sys_user u ON c.user_id = u.id " +
            "WHERE c.artwork_id = #{artworkId} " +
            "ORDER BY c.create_time DESC")
    List<CommentVO> selectCommentsWithUser(Long artworkId);
}