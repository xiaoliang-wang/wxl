package org.wxl.httpclient.model;

import org.apache.http.impl.client.CloseableHttpClient;


public class HttpClientModel {

    private CloseableHttpClient httpClient;

    private Long lut;

    private Integer port;

    private String hostname;

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Long getLut() {
        return lut;
    }

    public void setLut(Long lut) {
        this.lut = lut;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public HttpClientModel(String hostname,Integer port,CloseableHttpClient httpClient,Long lut){
        this.hostname = hostname;
        this.port = port;
        this.httpClient = httpClient;
        this.lut = lut;
    }
}
