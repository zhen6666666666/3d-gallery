package com.example.artwork_3d_frontend.controller;

import com.example.artwork_3d_frontend.dto.CommentDTO;
import com.example.artwork_3d_frontend.service.ArtworkInteractionService;
import com.example.artwork_3d_frontend.vo.ArtworkInteractionVO;
import com.example.artwork_3d_frontend.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artworks")
public class ArtworkInteractionController {

    @Autowired
    private ArtworkInteractionService interactionService;

    // 假设 SecurityContext 保存的用户 Details 中获取 userId，也可视你的 JwtUtils 自行调整获取逻辑
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        try {
            return Long.parseLong(authentication.getName()); // 对应 JWT 保存的 userId
        } catch (Exception e) {
            return null;
        }
    }

    // 1. 点赞 / 取消点赞
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable("id") Long artworkId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body("请先登录");
        boolean isLiked = interactionService.toggleLike(artworkId, userId);
        return ResponseEntity.ok(Map.of("isLiked", isLiked));
    }

    // 2. 收藏 / 取消收藏
    @PostMapping("/{id}/favorite")
    public ResponseEntity<?> toggleFavorite(@PathVariable("id") Long artworkId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body("请先登录");
        boolean isFavorited = interactionService.toggleFavorite(artworkId, userId);
        return ResponseEntity.ok(Map.of("isFavorited", isFavorited));
    }

    // 3. 发表评论
    @PostMapping("/comments")
    public ResponseEntity<?> addComment(@RequestBody CommentDTO dto, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body("请先登录");
        interactionService.addComment(dto, userId);
        return ResponseEntity.ok("评论成功");
    }

    // 4. 删除评论
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return ResponseEntity.status(401).body("请先登录");
        interactionService.deleteComment(commentId, userId);
        return ResponseEntity.ok("删除成功");
    }

    // 5. 获取指定作品的评论列表
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentVO>> getComments(@PathVariable("id") Long artworkId) {
        return ResponseEntity.ok(interactionService.getComments(artworkId));
    }

    // 6. 获取作品交互状态（点赞数、收藏数、当前用户是否已赞/已收藏）
    @GetMapping("/{id}/status")
    public ResponseEntity<ArtworkInteractionVO> getStatus(@PathVariable("id") Long artworkId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(interactionService.getInteractionStatus(artworkId, userId));
    }
}