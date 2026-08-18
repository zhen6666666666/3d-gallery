package com.example.artwork_3d_frontend.controller;

import com.example.artwork_3d_frontend.dto.ArtworkPublishDTO;
import com.example.artwork_3d_frontend.entity.Artwork;
import com.example.artwork_3d_frontend.service.ArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/artworks")
@CrossOrigin
public class ArtworkController {

    @Autowired
    private ArtworkService artworkService;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 本地保存路径（可根据实际情况调整）
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String newFileName = UUID.randomUUID().toString() + suffix;

        // 保存文件
        File destFile = new File(uploadDir + newFileName);
        file.transferTo(destFile);

        // 返回可访问的图片 URL（或根据前端格式包装为 Result 结构）
        return "/uploads/" + newFileName;
    }

    /**
     * 获取所有画作（按 3D 展位升序）
     */
    @GetMapping
    public List<Artwork> getAllArtworks() {
        return artworkService.getAllArtworksSorted();
    }

    /**
     * 发布作品接口
     */
    @PostMapping("/publish")
    public Artwork publishArtwork(@RequestBody ArtworkPublishDTO dto, HttpServletRequest request) {
        // 从 JwtAuthenticationFilter 写入的 request 属性中获取当前登录用户 ID
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            userId = 1L; // 未携带 Token 时的测试兜底 ID
        }
        return artworkService.publishArtwork(userId, dto);
    }

    /**
     * 兼容创建画作接口
     */
    @PostMapping
    public Artwork createArtwork(@RequestBody Artwork artwork) {
        artworkService.save(artwork);
        return artwork;
    }

    @GetMapping("/my/published")
    public List<Artwork> getMyPublished(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return artworkService.getMyArtworks(userId);
    }

    @GetMapping("/my/likes")
    public List<Artwork> getMyLikes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return artworkService.getMyLikedArtworks(userId);
    }

    @GetMapping("/my/favorites")
    public List<Artwork> getMyFavorites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return artworkService.getMyFavoritedArtworks(userId);
    }
}