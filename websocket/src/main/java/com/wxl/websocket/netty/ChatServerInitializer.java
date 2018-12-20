package com.wxl.websocket.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;
import com.wxl.websocket.http.HttpRequestHandler;
import com.wxl.websocket.model.ChannelInfo;
import com.wxl.websocket.model.RequestInfo;
import com.wxl.websocket.socket.TextWebSocketFrameHandler;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 王晓亮
 * @date 2018/8/30 18:07
 *
 * 扩展ChannelInitializer
 */
public class ChatServerInitializer extends ChannelInitializer<Channel> {


    private final ChannelGroup channelGroup;
    private final ConcurrentMap<String, List<ChannelInfo>> userChannelMap;
    private final String wsPath;
    private final ConcurrentMap<String, RequestInfo> channelUserMap;
    private final SocketEvent socketEvent;
    private final BusinessEvent businessEvent;

    public ChatServerInitializer(ChannelGroup channelGroup, ConcurrentMap<String, List<ChannelInfo>> userChannelMap, String wsPath, ConcurrentMap<String, RequestInfo> channelUserMap, SocketEvent socketEvent, BusinessEvent businessEvent) {
        this.channelGroup = channelGroup;
        this.userChannelMap = userChannelMap;
        this.wsPath = wsPath;
        this.channelUserMap = channelUserMap;
        this.socketEvent = socketEvent;
        this.businessEvent = businessEvent;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
//        将所有需要的ChannelHandler添加到ChannelPipeline
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
//        文件服务通道，文件大小限制
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
//        http请求通道，http升级到websocket协议的url
        pipeline.addLast(new HttpRequestHandler(wsPath,channelUserMap,socketEvent,businessEvent));
//        websocket协议通道，websocket协议url
        pipeline.addLast(new WebSocketServerProtocolHandler(wsPath));

        pipeline.addLast(new TextWebSocketFrameHandler(channelGroup,userChannelMap,channelUserMap,socketEvent,businessEvent));
    }
}
