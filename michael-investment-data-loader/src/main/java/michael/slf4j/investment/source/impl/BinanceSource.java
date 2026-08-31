package michael.slf4j.investment.source.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;  // 添加，用于优雅关闭

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;      // 新增
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.source.ISource;

@Component("binanceSource")
public class BinanceSource implements ISource, Closeable {
    private static final Logger log = Logger.getLogger(BinanceSource.class);
    private static final String URL_TEMPLATE = "https://api.binance.com/api/v3/klines?";
    
    // 1. 将 httpClient 作为成员变量，并在构造函数中初始化（带代理）
    private CloseableHttpClient httpClient;

    // 2. 构造函数：创建带代理的 HttpClient（单例，线程安全）
    public BinanceSource() {
        HttpHost proxy = new HttpHost("127.0.0.1", 7897, "http");
        this.httpClient = HttpClientBuilder.create()
                .setProxy(proxy)
                .setConnectionTimeToLive(30, TimeUnit.SECONDS) // 可选：连接存活时间
                .build();
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Override
    public String getContent(Set<String> securities) throws IOException {
        throw new RuntimeException("Unsupport set of security to get historical data");
    }

    @Override
    public String getContent(String security) throws IOException {
        return getContent(security, FreqEnum._15MI);
    }
    
    public String getContent(String security, FreqEnum freq) throws IOException {
        String symbol = security;
        
        String interval = freq.getValue().toLowerCase();
        if(freq == FreqEnum._1D) {
            interval = "1d";
        }
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("symbol", symbol);
        querys.put("interval", interval);
        querys.put("limit", "100");
        String params = querys.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        
        String url = URL_TEMPLATE + params;
//        log.info("Current URL:" + url);
        HttpGet httpGet = new HttpGet(url);
        
        // 3. 使用成员变量 httpClient，而不是每次创建新的
        int retry = 3;
        String content = null;
        while(retry > 0) {
            try(CloseableHttpResponse response = httpClient.execute(httpGet)){
                int status = response.getStatusLine().getStatusCode();
                if(status >= 400) {
                    log.error("[" + symbol + "] status:" + status);
                }
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    content = EntityUtils.toString(responseEntity).trim();
                }
                break;
            } catch(Exception e) {
                try {
                    TimeUnit.SECONDS.sleep(1L);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
                log.error("[" + retry + "]Retry for " + freq.getValue(), e);
                retry--;
            }
        }
        if(retry == 0) {
            throw new RuntimeException("Error when retrieving data for " + freq.getValue());
        }
        return content;
    }
}