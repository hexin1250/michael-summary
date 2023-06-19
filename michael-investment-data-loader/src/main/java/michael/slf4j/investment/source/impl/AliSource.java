package michael.slf4j.investment.source.impl;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.source.ISource;

@Component("aliSource")
public class AliSource implements ISource, Closeable {
	private static final Logger log = Logger.getLogger(AliSource.class);
	private CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	
	public String getContent(Set<String> securities) throws IOException {
		log.info("request to Ali server now.");
//		String[] symbolArr = new String[] {"SH000001","SH600887"};
//		String[] symbolArr = new String[] {"DCEI2209","DCEI2301","DCEJ2209","DCEJ2301","DCERB2210"};
		String symbol = securities.stream().map(security -> {
			if(security.startsWith("I") || security.startsWith("J")) {
				return "DCE" + security;
			} else if(security.startsWith("RB")) {
				return "SHFE" + security;
			}
			return security;
		}).collect(Collectors.joining(","));
		
//		HttpGet httpGet = new HttpGet("http://alirmcom2.market.alicloudapi.com/query/comrms?symbols=" + symbol);
		HttpGet httpGet = new HttpGet("http://alirmgbft.market.alicloudapi.com/query/comrms?symbols=" + symbol);
		Map<String, String> headers = new HashMap<String, String>();
	    //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
	    headers.put("Authorization", "APPCODE xxx");
	    for (Entry<String, String> entry : headers.entrySet()) {
	    	httpGet.setHeader(entry.getKey(), entry.getValue());
		}
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
	
	public String getContent(String security) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public static void main(String[] args) throws IOException {
		try(AliSource source = new AliSource()){
			Set<String> securities = Set.of("I2309", "RB2310", "J2309");
			String result = source.getContent(securities);
			String outputFileName = "output/result2.json";
			try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)))){
				bw.write(result);
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		httpClient.close();
	}
}
