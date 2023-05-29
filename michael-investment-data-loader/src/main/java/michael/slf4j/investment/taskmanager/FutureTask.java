package michael.slf4j.investment.taskmanager;

import java.io.IOException;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.source.ISource;
import michael.slf4j.investment.util.TradeUtil;

@Component("futureTask")
public class FutureTask implements Runnable {
	private static final Logger log = Logger.getLogger(FutureTask.class);

	@Autowired
	private FutureLoader futureLoader;
	
	@Autowired
	private ISource aliSource;
	
	@Autowired
	private IParser aliParser;
	
	private Set<String> securities = new HashSet<>();

	@Override
	public void run() {
		try {
			String content = aliSource.getContent(securities);
			LocalTime lt = LocalTime.now();
			if(lt.getSecond() == 0 && content.length() > 0 && TradeUtil.isTradingTime()) {
				futureLoader.loadMultiSecurities(aliParser, content, FreqEnum._TICK);
			}
		} catch (IOException e) {
			log.error("错误发生！", e);
		}
	}
	
	public void adjustSecurities(Set<String> adjustList) {
		adjustList.stream()
			.filter(security -> !securities.contains(security))
			.forEach(security -> {
				securities.add(security);
			});
	}
	
	@Bean(name="currentSecurities")
	public Set<String> getCurrentSecurities() {
		return securities;
	}
	
	public void clear() {
		securities.clear();
	}

}
