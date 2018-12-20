package com.wxl.websocket.model;

import io.netty.channel.ChannelId;

/**
 * @author 王晓亮
 * @date 2018/8/31 14:48
 */
public class ChannelInfo {

    private final long creationTime;

    private final String clientType;

    private final ChannelId channelId;

    public long getCreationTime() {
        return creationTime;
    }

    public String getClientType() {
        return clientType;
    }

    public ChannelId getChannelId() {
        return channelId;
    }

    public ChannelInfo(String clientType, ChannelId channelId){
        this.creationTime = System.currentTimeMillis();
        this.clientType = clientType;
        this.channelId = channelId;
    }

}
