package michael.slf4j.investment.source;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class HTTPCheck {
	private static final Logger log = Logger.getLogger(SinaSource.class);

	public static void main(String[] args) throws IOException {
		try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()){
			HttpPost request = new HttpPost("https://ft.10jqka.com.cn/api/v1/get_access_token");
			request.setHeader("Content-Type", "application/json");
			request.setHeader("refresh_token", "user_refresh_token");
			try(CloseableHttpResponse response = httpClient.execute(request)){
				int status = response.getStatusLine().getStatusCode();
				if(status >= 400) {
					log.error("status:" + status);
				}
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String line = EntityUtils.toString(responseEntity).trim();
					log.info(line);
				}
			}
		}
	}

}
