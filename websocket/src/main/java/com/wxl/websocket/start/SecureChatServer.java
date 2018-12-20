package com.wxl.websocket.start;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;
import com.wxl.websocket.model.ChannelInfo;
import com.wxl.websocket.model.RequestInfo;
import com.wxl.websocket.netty.SecureChatServerInitializer;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 王晓亮
 * @date 2018/8/30 18:07
 *
 * 扩展ChatServer以支持加密,没有经过测试
 */
@Deprecated
public class SecureChatServer extends ChatServer {
    private final SslContext context;

    private SecureChatServer(SslContext context, SocketEvent socketEvent) {
        super(socketEvent);
        this.context = context;
    }

    @Override
    protected ChannelInitializer<Channel> createInitializer(ChannelGroup channelGroup, ConcurrentMap<String, List<ChannelInfo>> userChannelMap, String wsPath, ConcurrentMap<String, RequestInfo> channelUserMap, SocketEvent socketEvent, BusinessEvent businessEvent) {
        return new SecureChatServerInitializer(channelGroup,userChannelMap, context,wsPath,channelUserMap,socketEvent,businessEvent);
    }


    /**
     * 对外暴露的websocket启动方法
     * @param port 服务占用的端口
     * @param wsPath 服务监听的url
     * @param socketEvent websocket相关事件的处理类
     * @return 对外暴露的对channel操作的接口
     */
    public static BusinessEvent start(int port, String wsPath, SocketEvent socketEvent){
        try {
            SelfSignedCertificate cert = new SelfSignedCertificate();
            SslContext context = SslContext.newServerContext(cert.certificate(), cert.privateKey());
            final SecureChatServer endpoint = new SecureChatServer(context,socketEvent);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    endpoint.destroy();
                }
            });
            return endpoint.run(new InetSocketAddress(port),wsPath,socketEvent);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (SSLException e) {
            e.printStackTrace();
        }

        return null;
    }

}
