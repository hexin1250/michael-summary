package michael.slf4j.investment.etl;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.FutureSecurityEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.source.ISource;
import michael.slf4j.investment.source.impl.AliHistoricalDataSource;
import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.DataLoaderUtil;
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
	
	private Map<FutureSecurityEnum, String> mainSecurityMap = new HashMap<>();
	private Map<FutureSecurityEnum, List<String>> securitiesMap = new HashMap<>();
	
	public void init() {
		Arrays.stream(FutureSecurityEnum.values()).parallel().forEach(e -> {
			List<String> securityList = e.getSecurities();
			String mainSecurity = getMainSecurity(securityList);
			mainSecurityMap.put(e, mainSecurity);
			Iterator<String> it = securityList.iterator();
			while(it.hasNext()) {
				String security = it.next();
				if(security.compareTo(mainSecurity) < 0) {
					it.remove();
				}
			}
			securitiesMap.put(e, securityList);
		});
		log.info("Current security list:" + securitiesMap);
	}
	
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
		if(TradeUtil.isUpdate15MinData()) {
			update15MinData();
		}
	}
	
	public void reload(String securityStr) {
		try {
			FreqEnum freq = FreqEnum._15MI;
			for (int i = 9; i >= 1; i--) {
				String content = aliHistoricalSource.getContent(securityStr, freq, i + "");
				Security security = new Security(securityStr, Variety.of(securityStr.substring(0, securityStr.length() - 4)));
				List<Timeseries> series = aliHistoricalParser.parse(security, content, freq);
				futureLoader.loadSecurity(security, freq, series);
				
				List<Timeseries> min30Series = get30MinBy15Min(security, freq, 50);
				futureLoader.loadSecurity(security, FreqEnum._30MI, min30Series);
			}
		} catch (IOException e) {
		}
	}
	
	public void update15MinData() {
		securitiesMap.entrySet().parallelStream().forEach(entry -> {
			try {
				for (String securityStr : entry.getValue()) {
					load15MiData(securityStr);
				}
			} catch (IOException | JMSException e) {
			}
		});
	}

	public void load15MiData(String securityStr) throws IOException, JMSException {
		FreqEnum freq = FreqEnum._15MI;
		String content = aliHistoricalSource.getContent(securityStr, freq);
		Security security = new Security(securityStr, Variety.of(securityStr.substring(0, securityStr.length() - 4)));
		List<Timeseries> series = aliHistoricalParser.parse(security, content, freq);
		for (int i = 1; i <= series.size(); i++) {
			Timeseries ts = series.get(i - 1);
			if(ts.getTradeTs().compareTo(new Date()) <= 0) {
				List<Timeseries> list = timeseriesRepository.getTimeseries(securityStr, ts.getTradeTs());
				if(!list.isEmpty()) {
					Timeseries ts1Min = list.get(0);
					ts.setOpenInterest(ts1Min.getOpenInterest());
				}
			}
		}
		futureLoader.loadSecurity(security, freq, series);
		messageService.send("future-15M-topic", series);
		
		List<Timeseries> min30Series = get30MinBy15Min(security, freq, 50);
		futureLoader.loadSecurity(security, FreqEnum._30MI, min30Series);
		if(TradeUtil.isUpdate30MinData()) {
			messageService.send("future-30M-topic", min30Series);
		}
	}
	
	private List<Timeseries> get30MinBy15Min(Security security, FreqEnum freq, int limit) {
		List<Timeseries> series = timeseriesRepository.findBySecurityFreqLimit(security.getName(), freq.getValue(), limit);
		Collections.sort(series, new Comparator<>() {
			@Override
			public int compare(Timeseries o1, Timeseries o2) {
				return o1.getTradeTs().compareTo(o2.getTradeTs());
			}
		});
		List<Timeseries> list = DataLoaderUtil.generate30TsListBy15ForRealTime(series);
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
	
	public void cleanup() {
		Arrays.stream(FutureSecurityEnum.values()).forEach(e -> {
			String mainSecurity = getMainSecurity(e.getSecurities());
			log.info("Start to cleanup:" + mainSecurity);
			String freq = FreqEnum._15MI.getValue();
			List<Timestamp> timestampList = timeseriesRepository.getDeplicatedTS(mainSecurity, freq);
			log.info("There are " + timestampList.size() + " to be deleted.");
			List<Timeseries> deleteList = new ArrayList<>();
			for (Timestamp timestamp : timestampList) {
				List<Timeseries> tsList = timeseriesRepository.getBySecurityFreqTs(mainSecurity, freq, timestamp);
				for (int i = 0; i < tsList.size() - 1; i++) {
					deleteList.add(tsList.get(i));
					if(deleteList.size() % 100 == 0) {
						log.info("Identify " + deleteList.size() + ".");
					}
				}
			}
			log.info("Final size:" + deleteList.size());
			timeseriesRepository.deleteAll(deleteList);
			log.info("Complete to cleanup:" + mainSecurity);
			
			List<Timeseries> staledList = timeseriesRepository.getStaledData(mainSecurity, freq);
			log.info("There are " + staledList.size() + " staled data");
			for (Timeseries ts : staledList) {
				List<Timeseries> min1List = timeseriesRepository.getTimeseries(mainSecurity, ts.getTradeTs());
				ts.setOpenInterest(min1List.get(0).getOpenInterest());
			}
			timeseriesRepository.saveAll(staledList);
			log.info("Complete to update staled data:" + mainSecurity);
			
			log.info("Correct 15Min data:" + mainSecurity);
			Timestamp timestamp = TradeUtil.getTimestamp(LocalDateTime.now());
			List<String> lastTradeDates = timeseriesRepository.getLast2TradeDate(e.name(), FreqEnum._1MI.getValue(), timestamp);
			String latestTradeDate = lastTradeDates.get(0);
			List<Timeseries> min15List = timeseriesRepository.findByTradeDateWithPeriod(mainSecurity, latestTradeDate, freq);
			for (Timeseries ts : min15List) {
				List<Timeseries> min1List = timeseriesRepository.getTimeseries(mainSecurity, ts.getTradeTs());
				ts.setOpenInterest(min1List.get(0).getOpenInterest());
			}
			timeseriesRepository.saveAll(min15List);
			log.info("Done to correct 15Min data:" + mainSecurity);
		});
	}
	
	private String getMainSecurity(List<String> securityList) {
		return getMainSecurity(LocalDateTime.now(), securityList);
	}
	
	private String getMainSecurity(LocalDateTime current, List<String> securityList) {
		Timestamp ts = TradeUtil.getTimestamp(current);
		String mainSecurity = null;
		double maxOpenInterest = 0D;
		for (String security : securityList) {
			Double openInterest = timeseriesRepository.getLastOpenInterest(security, ts);
			if (mainSecurity == null) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
			if (openInterest > maxOpenInterest) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
		}
		return mainSecurity;
	}

}
