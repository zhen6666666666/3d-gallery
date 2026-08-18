package com.example.artwork_3d_frontend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.artwork_3d_frontend.dto.CommentDTO;
import com.example.artwork_3d_frontend.entity.*;
import com.example.artwork_3d_frontend.mapper.*;
import com.example.artwork_3d_frontend.service.ArtworkInteractionService;
import com.example.artwork_3d_frontend.vo.ArtworkInteractionVO;
import com.example.artwork_3d_frontend.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArtworkInteractionServiceImpl implements ArtworkInteractionService {

    @Autowired
    private ArtworkMapper artworkMapper;
    @Autowired
    private ArtworkLikeMapper likeMapper;
    @Autowired
    private ArtworkFavoriteMapper favoriteMapper;
    @Autowired
    private ArtworkCommentMapper commentMapper;

    @Override
    @Transactional
    public boolean toggleLike(Long artworkId, Long userId) {
        LambdaQueryWrapper<ArtworkLike> wrapper = new LambdaQueryWrapper<ArtworkLike>()
                .eq(ArtworkLike::getArtworkId, artworkId)
                .eq(ArtworkLike::getUserId, userId);
        ArtworkLike existLike = likeMapper.selectOne(wrapper);

        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) throw new RuntimeException("作品不存在");

        if (existLike != null) {
            // 已点赞 -> 取消点赞
            likeMapper.deleteById(existLike.getId());
            artwork.setLikeCount(Math.max(0, (artwork.getLikeCount() == null ? 1 : artwork.getLikeCount()) - 1));
            artworkMapper.updateById(artwork);
            return false;
        } else {
            // 未点赞 -> 添加点赞
            ArtworkLike newLike = new ArtworkLike();
            newLike.setArtworkId(artworkId);
            newLike.setUserId(userId);
            newLike.setCreateTime(LocalDateTime.now());
            likeMapper.insert(newLike);

            artwork.setLikeCount((artwork.getLikeCount() == null ? 0 : artwork.getLikeCount()) + 1);
            artworkMapper.updateById(artwork);
            return true;
        }
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long artworkId, Long userId) {
        LambdaQueryWrapper<ArtworkFavorite> wrapper = new LambdaQueryWrapper<ArtworkFavorite>()
                .eq(ArtworkFavorite::getArtworkId, artworkId)
                .eq(ArtworkFavorite::getUserId, userId);
        ArtworkFavorite existFav = favoriteMapper.selectOne(wrapper);

        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) throw new RuntimeException("作品不存在");

        if (existFav != null) {
            // 已收藏 -> 取消收藏
            favoriteMapper.deleteById(existFav.getId());
            artwork.setFavoriteCount(Math.max(0, (artwork.getFavoriteCount() == null ? 1 : artwork.getFavoriteCount()) - 1));
            artworkMapper.updateById(artwork);
            return false;
        } else {
            // 未收藏 -> 添加收藏
            ArtworkFavorite newFav = new ArtworkFavorite();
            newFav.setArtworkId(artworkId);
            newFav.setUserId(userId);
            newFav.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(newFav);

            artwork.setFavoriteCount((artwork.getFavoriteCount() == null ? 0 : artwork.getFavoriteCount()) + 1);
            artworkMapper.updateById(artwork);
            return true;
        }
    }

    @Override
    public void addComment(CommentDTO dto, Long userId) {
        ArtworkComment comment = new ArtworkComment();
        comment.setArtworkId(dto.getArtworkId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        ArtworkComment comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getUserId().equals(userId)) {
            commentMapper.deleteById(commentId);
        } else {
            throw new RuntimeException("无权删除该评论");
        }
    }

    @Override
    public List<CommentVO> getComments(Long artworkId) {
        return commentMapper.selectCommentsWithUser(artworkId);
    }

    @Override
    public ArtworkInteractionVO getInteractionStatus(Long artworkId, Long userId) {
        ArtworkInteractionVO vo = new ArtworkInteractionVO();
        Artwork artwork = artworkMapper.selectById(artworkId);

        vo.setLikeCount(artwork != null && artwork.getLikeCount() != null ? artwork.getLikeCount() : 0);
        vo.setFavoriteCount(artwork != null && artwork.getFavoriteCount() != null ? artwork.getFavoriteCount() : 0);

        if (userId == null) {
            vo.setIsLiked(false);
            vo.setIsFavorited(false);
            return vo;
        }

        Long likeCount = likeMapper.selectCount(new LambdaQueryWrapper<ArtworkLike>()
                .eq(ArtworkLike::getArtworkId, artworkId)
                .eq(ArtworkLike::getUserId, userId));
        vo.setIsLiked(likeCount > 0);

        Long favCount = favoriteMapper.selectCount(new LambdaQueryWrapper<ArtworkFavorite>()
                .eq(ArtworkFavorite::getArtworkId, artworkId)
                .eq(ArtworkFavorite::getUserId, userId));
        vo.setIsFavorited(favCount > 0);

        return vo;
    }
}