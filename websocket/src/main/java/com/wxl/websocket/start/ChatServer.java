package com.wxl.websocket.start;

import com.wxl.websocket.event.BusinessEvent;
import com.wxl.websocket.event.SocketEvent;
import com.wxl.websocket.event.impl.BusinessEventImpl;
import com.wxl.websocket.model.ChannelInfo;
import com.wxl.websocket.model.RequestInfo;
import com.wxl.websocket.netty.ChatServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 王晓亮
 * @date 2018/8/30 18:07
 *
 * websocket引导程序
 */
public class ChatServer {

    /**
     * 创建DefaultChannelGroup，用来保存所有的已经建立连接的websocketChannel
     */
    private static final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    /**
     * 用户唯一标识和连接通道映射
     */
    private static final ConcurrentMap<String, List<ChannelInfo>> userChannelMap = PlatformDependent.newConcurrentHashMap();
    /**
     * 连接通道和用户唯一标识映射
     */
    private static final ConcurrentMap<String, RequestInfo> channelUserMap = PlatformDependent.newConcurrentHashMap();
    /**
     * 接收客户端TCP连接的Reactor线程池
     */
    private static final EventLoopGroup bossGroup = new NioEventLoopGroup();
    /**
     * 处理IO读写的Reactor线程池
     */
    private static final EventLoopGroup workGroup = new NioEventLoopGroup();
    /**
     * 对外暴露的对channel操作的接口
     */
    private final BusinessEvent businessEvent;


    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServer.class);

    /**
     * 引导代码
     * @param address 服务地址
     * @param wsPath websocket服务监听的path
     * @param socketEvent websocket相关事件的处理类
     * @return 对外暴露的对channel操作的接口
     */
    protected BusinessEvent run(final InetSocketAddress address,final String wsPath,final SocketEvent socketEvent) {

//        使用异步线程启动websocket服务，因为closeFuture().sync()后面的代码不能执行。如果不使用异步线程就无法对外暴露businessEvent
        Thread websocketBootstrapThread = new Thread(() -> {
            LOGGER.info("websocket服务启动开始，host："+address.toString()+"；url:"+wsPath);

            Class channelClass = null;

//                String osName = System.getProperty("os.name").toLowerCase();
//                System.out.println("操作系统名称："+osName);
//                if(null != osName && !osName.isEmpty() && osName.indexOf("linux") >= 0){
//                    channelClass = EpollServerSocketChannel.class;
//                }else {
//                    channelClass = NioServerSocketChannel.class;
//                }

            channelClass = NioServerSocketChannel.class;

            try {
                new ServerBootstrap().group(bossGroup,workGroup)
                        .channel(channelClass)
                        .option(ChannelOption.SO_BACKLOG, 2048)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(592048))
                        .childHandler(createInitializer(channelGroup,userChannelMap,wsPath,channelUserMap,socketEvent,businessEvent)).bind(address).sync().channel().closeFuture().sync();
            } catch (InterruptedException e) {
                LOGGER.error("websocket服务启动失败", e);
            }
        });
        websocketBootstrapThread.setName("websocket-"+address.getPort()+"-"+ (wsPath.startsWith("/") ? wsPath.substring(1):wsPath) );
        websocketBootstrapThread.start();

        return businessEvent;

    }

    /**
     * 创建通道初始化器
     * @param channelGroup 通道set
     * @param userChannelMap 用户唯一标识和连接通道映射
     * @param wsPath websocket服务监听的path
     * @param channelUserMap 连接通道和用户唯一标识映射
     * @param socketEvent websocket相关事件的处理类
     * @param businessEvent 对外暴露的对channel操作的接口
     * @return 通道初始化器
     */
    protected ChannelInitializer<Channel> createInitializer(ChannelGroup channelGroup, ConcurrentMap<String, List<ChannelInfo>> userChannelMap, String wsPath, ConcurrentMap<String, RequestInfo> channelUserMap, SocketEvent socketEvent, BusinessEvent businessEvent) {
        return new ChatServerInitializer(channelGroup,userChannelMap,wsPath,channelUserMap,socketEvent,businessEvent);
    }

    protected void destroy() {
        try {
            channelGroup.close();
            LOGGER.info("channelGroup集合关闭");
        }catch (Exception e){
            LOGGER.error("停止服务时关闭channelGroup出错",e);
        }
        try {
            bossGroup.shutdownGracefully();
            LOGGER.info("bossGroup线程池处理完毕");
        }catch (Exception e){
            LOGGER.error("停止服务时关闭bossGroup出错",e);
        }
        try {
            workGroup.shutdownGracefully();
            LOGGER.info("workGroup线程池处理完毕");
        }catch (Exception e){
            LOGGER.error("停止服务时关闭workGroup出错",e);
        }

    }

    public ChatServer(SocketEvent socketEvent){
        this.businessEvent = new BusinessEventImpl(channelGroup,userChannelMap,socketEvent);
    }

    /**
     * 对外暴露的websocket启动方法
     * @param port 服务占用的端口
     * @param wsPath 服务监听的url
     * @param socketEvent websocket相关事件的处理类
     * @return 对外暴露的对channel操作的接口
     */
    public static BusinessEvent start(int port, String wsPath, SocketEvent socketEvent){
        final ChatServer endpoint = new ChatServer(socketEvent);
//        注册服务停止时的执行钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> endpoint.destroy()));
        return endpoint.run(new InetSocketAddress(port),wsPath,socketEvent);
    }
}
