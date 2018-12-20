package com.wxl.websocket.netty;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;
import com.wxl.websocket.model.ChannelInfo;
import com.wxl.websocket.model.RequestInfo;

import javax.net.ssl.SSLEngine;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 王晓亮
 * @date 2018/8/30 18:07
 *
 * 扩展ChatServerInitializer以添加加密
 */
@Deprecated
public class SecureChatServerInitializer extends ChatServerInitializer {

    private final SslContext context;

    public SecureChatServerInitializer(ChannelGroup channelGroup, ConcurrentMap<String, List<ChannelInfo>> userChannelMap, SslContext context, String wsPath, ConcurrentMap<String, RequestInfo> channelUserMap, SocketEvent socketEvent, BusinessEvent businessEvent) {
//        调用父类方法
        super(channelGroup,userChannelMap,wsPath,channelUserMap,socketEvent,businessEvent);
        this.context = context;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
//        调用父类方法
        super.initChannel(ch);

        SSLEngine engine = context.newEngine(ch.alloc());
        engine.setUseClientMode(false);
//        将SslHandler添加到ChannelPipeline中
        ch.pipeline().addFirst(new SslHandler(engine));
    }
}
