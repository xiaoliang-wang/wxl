package com.wxl.websocket.event;

import io.netty.channel.ChannelId;
import com.wxl.websocket.enumeration.SendModeEnum;

/**
 * @author 王晓亮
 * @date 2018/8/31 13:41
 */
public interface SocketEvent {

    /**
     * 根据token验证，返回用户唯一标识
     * @param businessEvent 对外暴露的对channel操作的接口
     * @param token 连接中的token参数
     * @return 用户唯一标识
     */
    String getUserIdByParameter(BusinessEvent businessEvent, String token);

    /**
     * 连接前事件
     * @param businessEvent 对外暴露的对channel操作的接口
     * @param userId 用户唯一标识
     * @param channelId 连接通道的唯一标识
     */
    void jointBefore(BusinessEvent businessEvent, String userId, ChannelId channelId);

    /**
     * 连接后事件
     * @param businessEvent 对外暴露的对channel操作的接口
     * @param userId 用户唯一标识
     * @param channelId 连接通道的唯一标识
     */
    void jointLater(BusinessEvent businessEvent, String userId, ChannelId channelId);

    /**
     * 断开后事件
     * @param businessEvent 对外暴露的对channel操作的接口
     * @param userId 用户唯一标识
     * @param channelId 连接通道的唯一标识
     */
    void offLater(BusinessEvent businessEvent, String userId, ChannelId channelId);

    /**
     * 收到消息后事件
     * @param businessEvent 对外暴露的对channel操作的接口
     * @param userId 用户唯一标识
     * @param clientType 客户端类型
     * @param msg 消息内容
     */
    void receiveMessages(BusinessEvent businessEvent, String userId, String clientType, String msg);


    void sendMessageFuture(SendModeEnum sendType, String userId, String clientType, String msg, boolean success);


}

