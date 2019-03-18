package org.wxl.httpclient.pool;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.wxl.httpclient.model.HttpClientModel;
import org.oversimplify.threadpool.ScheduledTaskPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HttpClientPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientPool.class);

    private static final int TIME_OUT = 10 * 1000;

    private static ConcurrentMap<String, HttpClientModel> httpClientMap = new ConcurrentHashMap<>();

    private static final Lock lock = new ReentrantLock();

    private static final Long period = 5*60*1000L;


    static{
        ScheduledTaskPools.punctualTask(() -> {
            LOGGER.info("执行清理无效httpclient");
            if(httpClientMap.isEmpty()){
                LOGGER.info("集合为空，不存在无效httpclient");
                return;
            }
            LOGGER.info("执行前httpclient数量："+httpClientMap.size());
            for (Map.Entry<String, HttpClientModel> entry : httpClientMap.entrySet()) {
                HttpClientModel httpClientModel = entry.getValue();
                if(System.currentTimeMillis() - httpClientModel.getLut() > period){
                    httpClientMap.remove(entry.getKey());
                }
            }
            LOGGER.info("执行后httpclient数量："+httpClientMap.size());
        },period,period);
    }

    public static void config(HttpRequestBase httpRequestBase) {
        // 配置请求的超时设置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(TIME_OUT)
                .setConnectTimeout(TIME_OUT).setSocketTimeout(TIME_OUT).build();

        httpRequestBase.setConfig(requestConfig);
        httpRequestBase.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:62.0) Gecko/20100101 Firefox/62.0");
        httpRequestBase.setHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        httpRequestBase.setHeader("Accept", "application/json, text/plain, */*");
        httpRequestBase.setHeader("Cache-Control", "no-cache");
        httpRequestBase.setHeader("Accept-Encoding", "gzip, deflate, br");
    }

    /**
     * 获取HttpClient对象
     */
    public static CloseableHttpClient getHttpClient(String url) {

        if(null == url){
            return null;
        }
        String[] splits = url.split("/");
        if(splits.length < 3){
            return null;
        }
        String key = splits[2];
        HttpClientModel httpClientModel = httpClientMap.get(key);
        if(null != httpClientModel && null != httpClientModel.getHttpClient()){
            httpClientModel.setLut(System.currentTimeMillis());
            return httpClientModel.getHttpClient();
        }
        lock.lock();
        try {
            httpClientModel = httpClientMap.get(key);
            if(null == httpClientModel || null == httpClientModel.getHttpClient()){
                int port = 80;
                if(url.startsWith("https")){
                    port = 443;
                }
                if (key.contains(":")) {
                    String[] arr = key.split(":");
                    String hostname = arr[0];
                    port = Integer.parseInt(arr[1]);
                    CloseableHttpClient httpClient = createHttpClient(200, 40, 100, hostname, port);
                    httpClientMap.put(key,new HttpClientModel(hostname,port,httpClient,System.currentTimeMillis()));
                    return httpClient;
                }else{
                    CloseableHttpClient httpClient = createHttpClient(200, 40, 100, key, port);
                    httpClientMap.put(key,new HttpClientModel(key,port,httpClient,System.currentTimeMillis()));
                    return httpClient;
                }
            }else {
                httpClientModel.setLut(System.currentTimeMillis());
                return httpClientModel.getHttpClient();
            }
        }finally {
            lock.unlock();
        }
    }

    /**
     * 创建HttpClient对象
     */
    private static CloseableHttpClient createHttpClient(int maxTotal,int maxPerRoute, int maxRoute, String hostname, int port) {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", plainsf).register("https", sslsf).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        // 将最大连接数增加
        cm.setMaxTotal(maxTotal);
        // 将每个路由基础的连接增加
        cm.setDefaultMaxPerRoute(maxPerRoute);
//        HttpHost httpHost = new HttpHost(hostname, port);
        HttpHost httpHost = new HttpHost(hostname, port);
        // 将目标主机的最大连接数增加
        cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);

        // 请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception,
                                        int executionCount, HttpContext context) {
                if (executionCount >= 5) {// 如果已经重试了5次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// SSL握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setRetryHandler(httpRequestRetryHandler).build();
        return httpClient;
    }

}
