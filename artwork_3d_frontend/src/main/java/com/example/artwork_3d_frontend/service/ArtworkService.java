package com.example.artwork_3d_frontend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.artwork_3d_frontend.dto.ArtworkPublishDTO;
import com.example.artwork_3d_frontend.entity.Artwork;

import java.util.List;

public interface ArtworkService extends IService<Artwork> {

    /**
     * 获取按展位序号（positionIndex）升序排列的所有画作
     */
    List<Artwork> getAllArtworksSorted();

    /**
     * 发布新作品（自动计算并递增 3D 展厅 positionIndex）
     */
    Artwork publishArtwork(Long userId, ArtworkPublishDTO dto);

    List<Artwork> getMyArtworks(Long userId);

    List<Artwork> getMyLikedArtworks(Long userId);

    List<Artwork> getMyFavoritedArtworks(Long userId);
}