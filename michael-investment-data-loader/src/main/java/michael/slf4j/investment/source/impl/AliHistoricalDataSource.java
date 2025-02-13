package michael.slf4j.investment.source.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.source.ISource;

@Component("aliHistoricalSource")
public class AliHistoricalDataSource implements ISource, Closeable {
	private static final Logger log = Logger.getLogger(AliHistoricalDataSource.class);
	private CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	
	@Override
	public void close() throws IOException {
		httpClient.close();
	}

	@Override
	public String getContent(Set<String> securities) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getContent(String security) throws IOException {
		return getContent(security, FreqEnum._15MI);
	}
	
	public String getContent(String security, FreqEnum freq) throws IOException {
		return getContent(security, freq, "1");
	}
	
	public String getContent(String security, FreqEnum freq, String pageNum) throws IOException {
		String symbol = null;
		if(security.startsWith("I") || security.startsWith("J")) {
			symbol = "DCE" + security;
		} else if(security.startsWith("RB")) {
			symbol = "SHFE" + security;
		}
		
	    Map<String, String> querys = new HashMap<String, String>();
	    querys.put("period", freq.getValue());
	    querys.put("pidx", pageNum);
	    querys.put("psize", "500");
	    querys.put("symbol", symbol);
	    querys.put("withlast", "1");
	    String params = querys.entrySet().stream()
	    		.map(entry -> entry.getKey() + "=" + entry.getValue())
	    		.collect(Collectors.joining("&"));
		
		HttpGet httpGet = new HttpGet("http://alirmgbft.market.alicloudapi.com/query/comkm?" + params);
		Map<String, String> headers = new HashMap<String, String>();
	    //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
	    headers.put("Authorization", "APPCODE dbf34af5855347bd81e71d077d932522");
	    for (Entry<String, String> entry : headers.entrySet()) {
	    	httpGet.setHeader(entry.getKey(), entry.getValue());
		}
	    CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		try(CloseableHttpResponse response = httpClient.execute(httpGet)){
			String content = null;
			int status = response.getStatusLine().getStatusCode();
			if(status >= 400) {
				log.error("[" + symbol + "] status:" + status);
			}
			HttpEntity responseEntity = response.getEntity();
			if (responseEntity != null) {
				content = EntityUtils.toString(responseEntity).trim();
			}
			return content;
		}
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		String security = "RB2505";
		try(AliHistoricalDataSource source = new AliHistoricalDataSource();) {
			String ret = source.getContent(security, FreqEnum._15MI);
			log.info(ret);
			System.out.println(ret);
		}
	}

}

