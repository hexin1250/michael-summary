package michael.slf4j.investment.etl;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.source.ISource;
import michael.slf4j.investment.source.impl.AliHistoricalDataSource;
import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.TradeUtil;

@Component("dataLoaderClient")
public class DataLoaderClient {
	private static final Logger log = Logger.getLogger(DataLoaderClient.class);
	
	@Autowired
	private TaskManager taskManager;
	
	@Autowired
	private FutureLoader futureLoader;
	
	@Autowired
	private TimeseriesRepository timeseriesRepository;
	
	@Autowired
	@Qualifier(value="aliSource")
	private ISource aliSource;
	
	@Autowired
	@Qualifier(value="aliParser")
	private IParser aliParser;
	
	@Autowired
	@Qualifier(value="aliHistoricalSource")
	private AliHistoricalDataSource aliHistoricalSource;
	
	@Autowired
	@Qualifier(value="aliHistoricalParser")
	private IParser aliHistoricalParser;
	
	@Autowired
	@Qualifier(value="currentSecurities")
	private Set<String> futureSecurities;
	
	@Autowired
	MessageService messageService;
	
	public void update1MinData() {
		if(!TradeUtil.isTradingTime()) {
			return;
		}
		taskManager.subscribeSecurities();
		List<Timeseries> series = null;
		try {
			String content = aliSource.getContent(futureSecurities);
			FreqEnum freq = FreqEnum._1MI;
			series = aliParser.parse(content, freq);
			futureLoader.loadMultiSecurities(series, freq);
		} catch (IOException e) {
			/**
			 * Should not find one security. Ignore this case.
			 */
		}
		try {
			messageService.send("future-MI-topic", series);
		} catch (JMSException e) {
			log.error("Error when sending message to topic", e);
		}
	}
	
	public void update15MinData() {
		if(!TradeUtil.isTradingTime()) {
			return;
		}
		taskManager.subscribeSecurities();
		futureSecurities.parallelStream().forEach(securityStr -> {
			try {
				FreqEnum freq = FreqEnum._15MI;
				String content = aliHistoricalSource.getContent(securityStr, freq);
				Security security = new Security(securityStr, Variety.of(securityStr.substring(0, securityStr.length() - 4)));
				List<Timeseries> series = aliHistoricalParser.parse(security, content, freq);
				futureLoader.loadSecurity(security, freq, series);
				messageService.send("future-15M-topic", series);
				
				List<Timeseries> min30Series = get30MinBy15Min(security, freq, 100);
				futureLoader.loadSecurity(security, FreqEnum._30MI, min30Series);
				if(TradeUtil.isUpdate30MinData()) {
					messageService.send("future-30M-topic", min30Series);
				}
			} catch (IOException | JMSException e) {
			}
		});
	}
	
	private List<Timeseries> get30MinBy15Min(Security security, FreqEnum freq, int limit) {
		List<Timeseries> series = timeseriesRepository.findBySecurityFreqLimit(security.getName(), freq.getValue(), limit);
		List<Timeseries> list = new ArrayList<Timeseries>();
		Timeseries ts = null;
		for (Timeseries min15Ts : series) {
			Timestamp timestamp = min15Ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			int hour = ldt.getHour();
			int min = ldt.getMinute();
			boolean sameCondition = false;
			if(min % 30 == 0 || (hour == 10 && min == 15) || ts == null) {
				ts = min15Ts.copy();
				if(min % 15 == 0) {
					ts.setVolume(new BigDecimal(0));
				}
				sameCondition = true;
			}
			if(min % 30 == 15) {
				ts.setOpen(min15Ts.getOpen());
				ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min15Ts.getHigh().doubleValue())));
				ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min15Ts.getLow().doubleValue())));
				ts.setVolume(ts.getVolume().add(min15Ts.getVolume()));
				ts.setFreq(FreqEnum._30MI.getValue());
				if(sameCondition) {
					LocalDateTime newLdt = ldt.plusMinutes(15);
					ts.setTradeTs(new Timestamp(TradeUtil.getLong(newLdt)));
				}
				list.add(ts);
				ts = null;
			}
		}
		return list;
	}

	public void init15MinData() {
		initData(FreqEnum._15MI);
	}
	
	public void init30MinData() {
		initData(FreqEnum._30MI);
	}

	private void initData(FreqEnum freq) {
		if(!TradeUtil.isTradingTime()) {
			return;
		}
		taskManager.subscribeSecurities();
		for (String securityStr : futureSecurities) {
			List<Timeseries> series = futureLoader.getSecuritySeries(securityStr, freq.getValue(), 50);
			try {
				String topic = "future-" + freq.getValue() + "-topic";
				messageService.send(topic, series);
			} catch (JMSException e) {
				log.error("Error when sending message to topic", e);
			}
		}
	}

}
