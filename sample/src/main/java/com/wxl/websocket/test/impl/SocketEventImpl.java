package com.wxl.websocket.test.impl;

import io.netty.channel.ChannelId;
import com.wxl.websocket.enumeration.SendModeEnum;
import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;

/**
 * @author 王晓亮
 * @date 2018/9/5 10:16
 */
public class SocketEventImpl implements SocketEvent {

//    校验连接请求，返回用户的唯一标识。返回值不为空表示通过校验
    @Override
    public String getUserIdByParameter(BusinessEvent businessEvent, String token) {
        if(null == token || token.length() == 0){
            return null;
        }
        return token;
    }

//    连接建立前执行的事件
    @Override
    public void jointBefore(BusinessEvent businessEvent, String userId, ChannelId channelId) {
        System.out.println("连接前事件，用户："+userId);
    }

//    连接建立后执行的事件
    @Override
    public void jointLater(BusinessEvent businessEvent, String userId, ChannelId channelId) {
        System.out.println("连接后事件，用户："+userId);
    }

//    断开连接后执行的事件
    @Override
    public void offLater(BusinessEvent businessEvent, String userId, ChannelId channelId) {
        System.out.println("断开后事件，用户："+userId);
    }

//    收到消息后执行的事件
    @Override
    public void receiveMessages(final BusinessEvent businessEvent, String userId, String clientType, String msg) {
        businessEvent.sendMsg(userId,"哥们儿，收到消息了。消息内容："+msg );
    }

    @Override
    public void sendMessageFuture(SendModeEnum sendType, String userId, String clientType, String msg, boolean success) {
        System.out.println("发送消息后事件，发送方式："+sendType.getName()+"；接收消息用户："+userId+"；接收消息客户端："+clientType+"；消息内容："+msg+"；发送结果："+success);
    }
}
