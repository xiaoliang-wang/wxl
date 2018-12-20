package com.wxl.websocket.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;
import com.wxl.websocket.model.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;

/**
 * @author 王晓亮
 * @date 2018/8/30 18:07
 *
 * 扩展SimpleChannelInboundHandler以处理FullHttpRequest消息
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    /**
     *  定义拦截的websocket的url
     */
    private final String wsUri;
    /**
     * 连接通道和用户唯一标识映射
     */
    private final ConcurrentMap<String, RequestInfo> channelUserMap;
    /**
     * websocket相关事件的处理类
     */
    private final SocketEvent socketEvent;
    /**
     * 对外暴露的对channel操作的接口
     */
    private final BusinessEvent businessEvent;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestHandler.class);

    public HttpRequestHandler(String wsUri, ConcurrentMap<String, RequestInfo> channelUserMap, SocketEvent socketEvent, BusinessEvent businessEvent) {

        this.wsUri = wsUri;
        this.channelUserMap = channelUserMap;
        this.socketEvent = socketEvent;
        this.businessEvent = businessEvent;
        LOGGER.info("初始化HttpRequestHandler");
    }

    /**
     * 异常处理方法
     * @param ctx 通道信息
     * @param cause 异常信息
     * @throws Exception 抛出异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        LOGGER.error("发送消息发生异常", cause);
    }

    /**
     * 根据入参校验，正确则返回用户唯一标识
     * @param token
     * @return 用户唯一标识
     */
    private String getUserIdByParameter(String token){
        return socketEvent.getUserIdByParameter(businessEvent,token);
    }

    /**
     * 接收连接请求，可以将http请求升级为websocket请求。netty5的方法，netty4中是channelRead0
     * @param channelHandlerContext 通道信息
     * @param fullHttpRequest 请求信息
     * @throws Exception super方法抛出的，暂时没有看
     */
//    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {

//        判断url是否正确,判断参数是否合法
        String url = fullHttpRequest.uri();
        if(null == url || url.length() <= ( wsUri.length()+1) || !url.startsWith(wsUri+"?") ){
            LOGGER.info("捕获到错误的请求url："+url);
            channelHandlerContext.close();
            return;
        }
//        解析参数，顺序为：token、clientType
        String[] parameter = url.substring(wsUri.length()+1).split(",");
        if(parameter.length < 2){
            LOGGER.info("捕获到错我的请求参数："+url.substring(wsUri.length()+1));
            channelHandlerContext.close();
            return;
        }

//        根据入参获取用户唯一标识，如果没有获取到则关闭连接
        String userId = getUserIdByParameter(parameter[0]);
        if(null == userId || userId.length() == 0){
            LOGGER.info("连接鉴权没有通过，url:"+url);
            channelHandlerContext.close();
            return;
        }

//        连接前执行的事件
        socketEvent.jointBefore(businessEvent,userId, channelHandlerContext.channel().id());

//        重置请求url
        fullHttpRequest.setUri(wsUri);
        channelUserMap.put(channelHandlerContext.channel().id().asLongText(), new RequestInfo(userId, parameter[0],parameter[1]));

//        fireChannelRead方法完成后将调用FullHttpRequest对象的release方法释放资源
        channelHandlerContext.fireChannelRead(fullHttpRequest.retain());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        messageReceived(channelHandlerContext,fullHttpRequest);
    }
}
