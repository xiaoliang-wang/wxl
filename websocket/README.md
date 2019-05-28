# oversimplify-websocket

# 项目介绍

    基于netty实现websocket服务框架，深度封装使用简单。一行代码完成websocket服务的搭建和启动。

    对外暴露接口直接针对业务系统用户进行操作，无需再去管理各种Channel


## 项目启动

    ```java

        final BusinessEvent businessEvent = ChatServer.start(8888, "/wxl",socketEvent);

    ```
    参数说明：
        8888 ： 服务占用的端口
        /wxl ： websocket连接的请求url
        socketEvent ： websocket事件接口的实现类对象，包括连接请求校验、连接前执行、连接后执行、断开后执行、收到消息后执行等。需要根据业务自行实现

## websocket事件接口说明

    ```java

        /**
         * 根据token验证，返回用户唯一标识
         * @param businessEvent 对外暴露的对channel操作的接口
         * @param token 连接中的token参数
         * @return 用户唯一标识
         */
        String getUserIdByParameter(BusinessEvent businessEvent,String token);

        /**
         * 连接前事件
         * @param businessEvent 对外暴露的对channel操作的接口
         * @param userId 用户唯一标识
         * @param channelId 连接通道的唯一标识
         */
        void jointBefore(BusinessEvent businessEvent,String userId, ChannelId channelId);

        /**
         * 连接后事件
         * @param businessEvent 对外暴露的对channel操作的接口
         * @param userId 用户唯一标识
         * @param channelId 连接通道的唯一标识
         */
        void jointLater(BusinessEvent businessEvent,String userId, ChannelId channelId);

        /**
         * 断开后事件
         * @param businessEvent 对外暴露的对channel操作的接口
         * @param userId 用户唯一标识
         * @param channelId 连接通道的唯一标识
         */
        void offLater(BusinessEvent businessEvent,String userId, ChannelId channelId);

        /**
         * 收到消息后事件
         * @param businessEvent 对外暴露的对channel操作的接口
         * @param userId 用户唯一标识
         * @param clientType 客户端类型
         * @param msg 消息内容
         */
        void receiveMessages(BusinessEvent businessEvent,String userId, String clientType, String msg);

    ```

## 对websocket连接进行操作

    使用在项目启动时得到的businessEvent对象可以进行消息广播、连接断开、向某个用户推送消息等操作。具体接口如下：

    ```java

        /**
         * 向某用户推送消息
         * @param userId 用户唯一标识
         * @param msg 消息内容
         * @return 是否操作成功
         */
        boolean sendMsg(String userId, String msg);

        /**
         * 向某用户的某种客户端推送消息
         * @param userId 用户唯一标识
         * @param clientType 客户端类型
         * @param msg 消息内容
         * @return 是否操作成功
         */
        boolean sendMsg(String userId,String clientType, String msg);

        /**
         * 广播消息
         * @param msg 消息内容
         * @return 是否操作成功
         */
        boolean broadcast(String msg);

        /**
         * 向某种客户端广播消息
         * @param clientType 客户端类型
         * @param msg 消息内容
         * @return 是否操作成功
         */
        boolean broadcast(String clientType,String msg);

        /**
         * 断开某个用户的所有连接
         * @param userId 用户唯一标识
         * @return 是否操作成功
         */
        boolean close(String userId);

        /**
         * 断开某个用户的某种客户端的连接
         * @param userId 用户唯一标识
         * @param clientType 客户端类型
         * @return 是否操作成功
         */
        boolean close(String userId,String clientType);

        /**
         * 断开指定连接
         * @param channelId 连接通道的唯一标识
         * @return 是否操作成功
         */
        boolean close(ChannelId channelId);

    ```

# 项目验证和调试

## 项目启动

    源码中的测试包中有开发测试类，可以执行该类启动项目进行调试。

    开发测试类在test包下 org.oversimplify.test.ChatServerTest

## web端调试

    因本项目没有提供web端，所以需要使用第三方的web页面进行调试。

    推荐调试地址：http://coolaf.com/tool/chattest  该页面禁止了使用http协议连接websocket服务，所以无法测试握手升级

    使用源码中org.oversimplify.test.ChatServerTest类启动项目，测试的websocket连接地址为：ws://localhost:8888/wxl?bbb,web

    url以及参数部分说明：{协议类型}://{host}:{port}/{url}?{token},{clientType}


## 模块化（netty5废弃，netty4对模块化支持的不好。所以暂时废弃）

    本项目支持java模块化，如果在模块化的项目中使用websocket-core模块请参考websocket-test模块的module-info.java



 