package com.example.artwork_3d_frontend.service;

import com.example.artwork_3d_frontend.dto.CommentDTO;
import com.example.artwork_3d_frontend.vo.ArtworkInteractionVO;
import com.example.artwork_3d_frontend.vo.CommentVO;

import java.util.List;

public interface ArtworkInteractionService {
    // 点赞切换（点赞/取消点赞）
    boolean toggleLike(Long artworkId, Long userId);

    // 收藏切换（收藏/取消收藏）
    boolean toggleFavorite(Long artworkId, Long userId);

    // 发表评论
    void addComment(CommentDTO dto, Long userId);

    // 删除评论
    void deleteComment(Long commentId, Long userId);

    // 获取某作品的评论列表
    List<CommentVO> getComments(Long artworkId);

    // 获取当前用户对作品的交互状态（是否已点赞/收藏）
    ArtworkInteractionVO getInteractionStatus(Long artworkId, Long userId);
}