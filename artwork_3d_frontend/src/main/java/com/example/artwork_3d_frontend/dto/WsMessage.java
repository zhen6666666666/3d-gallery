package com.example.artwork_3d_frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    /**
     * 消息类型:
     * - USER_JOIN  : 新用户进入展厅
     * - USER_LEAVE : 用户离开展厅
     * - USER_MOVE  : 用户移动/视角旋转
     * - INIT_USERS : 给刚进入的用户推当前所有在线玩家
     */
    private String type;
    private Long senderId;
    private Object data;
}