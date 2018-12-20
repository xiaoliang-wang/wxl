package com.wxl.websocket.socket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;
import com.wxl.websocket.http.HttpRequestHandler;
import com.wxl.websocket.model.ChannelInfo;
import com.wxl.websocket.model.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 王晓亮
 * @date 2018/8/30 18:07
 *
 * 扩展SimpleChannelInboundHandler以处理TextWebSocketFrame消息
 */
public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ChannelGroup channelGroup;
    private final ConcurrentMap<String, List<ChannelInfo>> userChannelMap;
    private final ConcurrentMap<String, RequestInfo> channelUserMap;
    private final SocketEvent socketEvent;
    private final BusinessEvent businessEvent;
    private static final Logger LOGGER = LoggerFactory.getLogger(TextWebSocketFrameHandler.class);

    public TextWebSocketFrameHandler(ChannelGroup channelGroup, ConcurrentMap<String, List<ChannelInfo>> userChannelMap, ConcurrentMap<String, RequestInfo> channelUserMap, SocketEvent socketEvent, BusinessEvent businessEvent) {
        this.channelGroup = channelGroup;
        this.userChannelMap = userChannelMap;
        this.channelUserMap = channelUserMap;
        this.socketEvent = socketEvent;
        this.businessEvent = businessEvent;
    }

    /**
     * 重写userEventTriggered方法以处理自定义事件
     * @param ctx 通道信息
     * @param evt 事件内容
     * @throws Exception 抛出异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(null != channelUserMap.get(ctx.channel().id().asLongText())){
            if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {

//              从该Channelipeline中移除HttpRequestHandler，因为不会再收到http消息了
                ctx.pipeline().remove(HttpRequestHandler.class);
//              将新的websocket连接添加到ChannelGroup，以便能够收到消息
                RequestInfo requestInfo = channelUserMap.get(ctx.channel().id().asLongText());
                if(putUserChannelMap(requestInfo.getUserId(),new ChannelInfo(requestInfo.getClientType(), ctx.channel().id()))){
                    channelGroup.add(ctx.channel());
                    socketEvent.jointLater(businessEvent,requestInfo.getUserId(), ctx.channel().id());
//                    ctx.channel().writeAndFlush(new TextWebSocketFrame("哥们儿，你连接成功了，channelId： " + ctx.channel().id()));

                }


            }else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }


    /**
     * 往userChannelMap中增加连接信息
     * @param key 用户唯一标识
     * @param value 连接信息
     * @return 操作是否成功
     */
    private synchronized boolean putUserChannelMap(String key,ChannelInfo value){
//        TODO 没有校验参数
        if(null == userChannelMap.get(key)){
            List<ChannelInfo> channelInfoList = new LinkedList<ChannelInfo>();
            channelInfoList.add(value);
            userChannelMap.put(key,channelInfoList);
            return true;
        }else {
            userChannelMap.get(key).add(value);
            return true;
        }

    }

    /**
     * websocket连接事件
     * @param ctx 通道信息
     * @throws Exception 抛出异常
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("捕获请求，客户端：" + ctx.channel().remoteAddress().toString());
    }


    /**
     * websocket断开事件
     * @param ctx 通道信息
     * @throws Exception 抛出异常
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {


        RequestInfo requestInfo = channelUserMap.remove(ctx.channel().id().asLongText());

        if( null == requestInfo || null == requestInfo.getUserId()){
            LOGGER.error("在channelUserMap中没有找到当前断开的连接和用户的映射关系");
            return;
        }

        List<ChannelInfo> channelInfoList = userChannelMap.get(requestInfo.getUserId());
        if( null == channelInfoList || channelInfoList.isEmpty()){
            LOGGER.error("在userChannelMap中没有该用户对应的socket连接通道信息");
            return;
        }
        for (ChannelInfo channelInfo : channelInfoList){
            if(channelInfo.getChannelId().equals(ctx.channel().id())){
                synchronized (this){
                    channelInfoList.remove(channelInfo);
                    if(channelInfoList.isEmpty()){
                        userChannelMap.remove(requestInfo.getUserId());
                    }
                }
                socketEvent.offLater(businessEvent,requestInfo.getUserId(), ctx.channel().id());
                return;
            }
        }

    }

    /**
     * websocket接收消息事件。netty5的方法，netty4中是channelRead0
     * @param channelHandlerContext 通道信息
     * @param textWebSocketFrame 消息内容
     * @throws Exception 抛出异常
     */
//    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        RequestInfo requestInfo = channelUserMap.get(channelHandlerContext.channel().id().asLongText());
        if( null == requestInfo || null == requestInfo.getUserId() ){
            LOGGER.error("接收到消息，但是无法映射到发消息的用户");
        }
//        接收到消息
        socketEvent.receiveMessages(businessEvent,requestInfo.getUserId(), requestInfo.getClientType(),textWebSocketFrame.text());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        messageReceived(channelHandlerContext,textWebSocketFrame);
    }
}
