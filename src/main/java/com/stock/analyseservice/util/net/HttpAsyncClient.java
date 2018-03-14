package com.stock.analyseservice.util.net;

import java.io.IOException;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步HTTP请求模块
 * 网络访问，端口扫描
 */
public class HttpAsyncClient implements IHttpAsyncClient {
    private final ExecutorService service;

    private Map<String, String> header;

    private Charset charset;

    private int timeout;

    public HttpAsyncClient(int threadNum, int timeout) {
        this(threadNum, null, Charset.defaultCharset(), timeout);
    }

    public HttpAsyncClient(int threadNum, Map<String, String> header, Charset charset, int timeout) {
        this.service = Executors.newFixedThreadPool(threadNum);
        this.header = header;
        this.charset = charset;
        this.timeout = timeout;
    }

    @Override
    public void handleGet(String url, CompletionHandler<Response, String> handler) {
        this.service.execute(() -> {
            try {
                Response res = NetUtil.get(url, this.header, this.charset, this.timeout);
                handler.completed(res, url);
            } catch (IOException e) {
                handler.failed(e, url);
            }
        });
    }

    @Override
    public void handleScanPort(String host, String port, CompletionHandler<Boolean, String> handler) {
        this.service.execute(() -> {
            try {
                String ip = NetUtil.conn(host, Integer.parseInt(port), this.timeout);
                if (ip != null && ip.length() > 0) {
                    handler.completed(true, ip);
                } else {
                    handler.completed(false, ip);
                }
            } catch (IOException e) {
                handler.failed(e, null);
            }
        });
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void close() {
        this.service.shutdownNow();
    }
}
