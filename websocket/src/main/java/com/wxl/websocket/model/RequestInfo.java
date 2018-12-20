package com.wxl.websocket.model;

/**
 * @author 王晓亮
 * @date 2018/9/3 15:04
 */
public class RequestInfo {

    private final long creationTime;

    private final String clientType;

    private final String userId;

    private final String token;


    public long getCreationTime() {
        return creationTime;
    }

    public String getClientType() {
        return clientType;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public RequestInfo(String userId, String token , String clientType){
        this.creationTime = System.currentTimeMillis();
        this.userId = userId;
        this.clientType = clientType;
        this.token = token;
    }
}
