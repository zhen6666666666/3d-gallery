package com.example.artwork_3d_frontend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.artwork_3d_frontend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}