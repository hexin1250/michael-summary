package michael.slf4j.investment.source;

import java.io.Closeable;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;

@Controller
public class SinaSource implements Closeable {
	private static final Logger log = Logger.getLogger(SinaSource.class);
	private static final Pattern p = Pattern.compile(".*[\"](.+)[\"].*");
	
	private CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	
	public String getContent(String security) throws IOException {
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

	@Override
	public void close() throws IOException {
		httpClient.close();
	}
}
