package com.wxl.websocket.enumeration;

/**
 * @author 王晓亮
 * @date 2018/9/11 14:28
 */
public enum SendModeEnum {

    SEND_BY_USER("推送到某用户","send_by_user"),
    SEND_BY_USER_AND_CLIENTTYPE("推送到某用户的某种客户端","send_by_user_and_clienttype"),
    BROADCAST("广播","broadcast"),
    BROADCAST_CLIENTTYPE("广播到某种客户端","broadcast_clienttype");

    private final String name;
    private final String code;

    SendModeEnum(String name,String code){
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }



}
