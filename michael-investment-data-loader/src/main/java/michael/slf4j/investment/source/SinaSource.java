package michael.slf4j.investment.source;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
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

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.util.TradeUtil;

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
				log.error("[" + security + "] status:" + status);
			}
			HttpEntity responseEntity = response.getEntity();
			if (responseEntity != null) {
				String line = EntityUtils.toString(responseEntity).trim();
				Matcher m = p.matcher(line);
				if(m.matches()) {
					content = m.group(1);
				} else {
					log.error("[" + security + "] doesn't match!" + line);
				}
			}
			return content;
		}
	}
	
	public Timeseries getModel(String security) throws IOException {
		String content = getContent(security);
		return generateModel(security, content);
	}
	
	private Timeseries generateModel(String security, String content) {
		String[] parts = content.split(",");
		Timeseries m = new Timeseries();
		m.setSecurity(security);
		m.setVariety(security.replaceAll("[\\d]+", ""));
		m.setSecurityName(parts[0]);
		m.setOpen(new BigDecimal(parts[2]));
		m.setHigh(new BigDecimal(parts[3]));
		m.setLow(new BigDecimal(parts[4]));
		m.setClose(new BigDecimal(parts[8]));
		m.setOpenInterest(new BigDecimal(parts[13]));
		m.setVolume(new BigDecimal(parts[14]));
		BigDecimal buy1 = new BigDecimal(parts[6]);
		BigDecimal sell1 = new BigDecimal(parts[7]);
		if (buy1.compareTo(new BigDecimal(0)) == 0) {
			m.setDownLimit(new BigDecimal(parts[8]));
		}
		if (sell1.compareTo(new BigDecimal(0)) == 0) {
			m.setUpLimit(new BigDecimal(parts[8]));
		}
		m.setFreq(FreqEnum._TICK.getValue());
		
		m.setTradeDate(TradeUtil.getDateStr(TradeUtil.getTradeDate()));
		m.setTradeTs(new Timestamp(System.currentTimeMillis()));
		return m;
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}
}
