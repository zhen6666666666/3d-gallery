package com.example.artwork_3d_frontend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.artwork_3d_frontend.dto.ArtworkPublishDTO;
import com.example.artwork_3d_frontend.entity.Artwork;
import com.example.artwork_3d_frontend.mapper.ArtworkMapper;
import com.example.artwork_3d_frontend.service.ArtworkService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArtworkServiceImpl extends ServiceImpl<ArtworkMapper, Artwork> implements ArtworkService {

    @Resource
    ArtworkMapper artworkMapper;

    @Override
    public List<Artwork> getAllArtworksSorted() {
        return this.list(new LambdaQueryWrapper<Artwork>()
                .orderByAsc(Artwork::getPositionIndex));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Artwork publishArtwork(Long userId, ArtworkPublishDTO dto) {
        // 1. 查询当前最大的 positionIndex，实现自动递增排序
        LambdaQueryWrapper<Artwork> query = new LambdaQueryWrapper<>();
        query.select(Artwork::getPositionIndex)
                .orderByDesc(Artwork::getPositionIndex)
                .last("LIMIT 1");

        Artwork lastArt = this.getOne(query);
        int nextPosition = (lastArt != null && lastArt.getPositionIndex() != null)
                ? lastArt.getPositionIndex() + 1
                : 0;

        // 2. 组装实体属性
        Artwork artwork = new Artwork();
        BeanUtils.copyProperties(dto, artwork);
        artwork.setUserId(userId);
        artwork.setPositionIndex(nextPosition);
        artwork.setLikeCount(0);
        artwork.setFavoriteCount(0);
        artwork.setCreateTime(LocalDateTime.now());

        // 3. 存入数据库
        this.save(artwork);
        return artwork;
    }

    @Override
    public List<Artwork> getMyArtworks(Long userId) {
        return artworkMapper.selectByUserId(userId);
    }

    @Override
    public List<Artwork> getMyLikedArtworks(Long userId) {
        return artworkMapper.selectLikedArtworksByUserId(userId);
    }

    @Override
    public List<Artwork> getMyFavoritedArtworks(Long userId) {
        return artworkMapper.selectFavoritedArtworksByUserId(userId);
    }
}