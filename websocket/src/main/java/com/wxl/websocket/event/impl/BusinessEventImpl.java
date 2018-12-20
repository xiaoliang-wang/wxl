package com.wxl.websocket.event.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GenericFutureListener;
import com.wxl.websocket.enumeration.SendModeEnum;
import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;
import com.wxl.websocket.model.ChannelInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 王晓亮
 * @date 2018/9/3 18:23
 */
public class BusinessEventImpl implements BusinessEvent {


    private final ChannelGroup channelGroup;
    private final ConcurrentMap<String, List<ChannelInfo>> userChannelMap;
    private final SocketEvent socketEvent;

    public BusinessEventImpl(ChannelGroup channelGroup, ConcurrentMap<String, List<ChannelInfo>> userChannelMap, SocketEvent socketEvent){

        this.channelGroup = channelGroup;
        this.userChannelMap = userChannelMap;
        this.socketEvent = socketEvent;

    }


    @Override
    public void sendMsg(String userId, String msg) {
        sendMsg(userId, null, msg);
    }

    @Override
    public void sendMsg(String userId, String clientType, String msg) {
        List<ChannelInfo> channelInfoList = userChannelMap.get(userId);
        if(null == channelInfoList || channelInfoList.isEmpty()){
            return;
        }
        for (ChannelInfo channelInfo: channelInfoList) {
            if(null == clientType || clientType.equals(channelInfo.getClientType())){
                final Channel channel = channelGroup.find(channelInfo.getChannelId());
                if(null != channel && channel.isOpen()){
//                    new TextWebSocketFrame(msg)不能在循环外实例化，否则出问题
                    channel.writeAndFlush(new TextWebSocketFrame(msg)).addListener(
                            (GenericFutureListener) future -> {
                                if(future.isSuccess()){
                                    socketEvent.sendMessageFuture(null == clientType? SendModeEnum.SEND_BY_USER:SendModeEnum.SEND_BY_USER_AND_CLIENTTYPE,userId ,channelInfo.getClientType() ,msg ,true );
                                }else {
                                    socketEvent.sendMessageFuture(null == clientType?SendModeEnum.SEND_BY_USER:SendModeEnum.SEND_BY_USER_AND_CLIENTTYPE,userId ,channelInfo.getClientType() ,msg ,false );
                                }
                            });
                }

            }
        }
    }

    @Override
    public void broadcast(String msg) {
        if(null == channelGroup || channelGroup.size() == 0){
            return;
        }
        channelGroup.writeAndFlush(new TextWebSocketFrame(msg)).addListener(
                (GenericFutureListener) future -> {
                    if(future.isSuccess()){
                        socketEvent.sendMessageFuture(SendModeEnum.BROADCAST,null ,null ,msg ,true );
                    }else {
                        socketEvent.sendMessageFuture(SendModeEnum.BROADCAST,null ,null ,msg ,false );
                    }
                }

        );
    }

    @Override
    public void broadcast(String clientType, String msg) {

//        TODO 需要优化，否则在并发量和连接数大了以后可能会出问题。优化方案：每种客户端的Channel放到一个channelGroup中

        for(Map.Entry<String,List<ChannelInfo>> channelInfoList : userChannelMap.entrySet()){

            if(null == channelInfoList.getValue() || channelInfoList.getValue().isEmpty()){
                continue;
            }
            for (ChannelInfo channelInfo: channelInfoList.getValue()) {
                if(clientType.equals(channelInfo.getClientType())){
//                    new TextWebSocketFrame(msg)不能在循环外实例化，否则出问题
                    channelGroup.find(channelInfo.getChannelId()).writeAndFlush(new TextWebSocketFrame(msg)).addListener(
                            (GenericFutureListener) future -> {
                                if(future.isSuccess()){
                                    socketEvent.sendMessageFuture(SendModeEnum.BROADCAST_CLIENTTYPE,channelInfoList.getKey() ,clientType ,msg ,true );
                                }else {
                                    socketEvent.sendMessageFuture(SendModeEnum.BROADCAST_CLIENTTYPE,channelInfoList.getKey() ,clientType ,msg ,false );
                                }
                            });

                }
            }

        }
    }

    @Override
    public void close(String userId) {
        close(userId,null );
    }

    @Override
    public void close(String userId, String clientType) {
        List<ChannelInfo> channelInfoList = userChannelMap.get(userId);
        if(null == channelInfoList || channelInfoList.isEmpty()){
            return;
        }
        for (ChannelInfo channelInfo: channelInfoList) {
            if(null == clientType || clientType.equals(channelInfo.getClientType())){
                close(channelInfo.getChannelId());
            }
        }
    }

    @Override
    public void close(ChannelId channelId) {
        if(null == channelId){
            return;
        }
        Channel channel = channelGroup.find(channelId);
        if(null != channel){
            channel.close();
        }
    }
}
