package michael.slf4j.investment.taskmanager;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.util.TradeUtil;

public class FutureTask implements Runnable {
	private static final Logger log = Logger.getLogger(FutureTask.class);
	private static final Pattern p = Pattern.compile(".*[\"](.+)[\"].*");

	private FutureLoader futureLoader;
	private CloseableHttpClient httpClient;
	private String security;
	
	public FutureTask(FutureLoader futureLoader, CloseableHttpClient httpClient, String security) {
		this.futureLoader = futureLoader;
		this.httpClient = httpClient;
		this.security = security;
	}

	@Override
	public void run() {
		try {
			String content = getContent(httpClient, security);
			if(content.length() > 0 && TradeUtil.isTradingTime()) {
				futureLoader.load(security, content);
			}
		} catch (IOException e) {
			log.error("错误发生！", e);
		}
	}
	
	public String getContent(CloseableHttpClient httpClient, String security) throws IOException {
		HttpGet httpGet = new HttpGet("http://hq.sinajs.cn/list=" + security);
		try(CloseableHttpResponse response = httpClient.execute(httpGet)){
			String content = null;
			int status = response.getStatusLine().getStatusCode();
			if(status >= 400) {
				log.error("[" + security + "]响应状态为:" + status);
			}
			HttpEntity responseEntity = response.getEntity();
			if (responseEntity != null) {
				String line = EntityUtils.toString(responseEntity).trim();
				Matcher m = p.matcher(line);
				if(m.matches()) {
					content = m.group(1);
				} else {
					log.error("[" + security + "]不匹配!" + line);
				}
			}
			return content;
		}
	}

}
