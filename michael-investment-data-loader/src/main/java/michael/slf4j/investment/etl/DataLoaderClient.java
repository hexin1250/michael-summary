package michael.slf4j.investment.etl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.constant.Constants;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.FutureSecurityEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.TopDeal;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.repo.TopDealRepository;
import michael.slf4j.investment.source.impl.AliHistoricalDataSourceV2;
import michael.slf4j.investment.source.impl.AliSourceV2;
import michael.slf4j.investment.source.impl.BinanceSource;
import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.DataLoaderUtil;
import michael.slf4j.investment.util.MyFileUtil;
import michael.slf4j.investment.util.SeleniumParser;
import michael.slf4j.investment.util.TradeUtil;

@Component("dataLoaderClient")
public class DataLoaderClient {
	private static final Logger log = Logger.getLogger(DataLoaderClient.class);
	private static final String FOLDER_PREFIX = "src/main/data/output";
	
	@Autowired
	private TaskManager taskManager;
	
	@Autowired
	private FutureLoader futureLoader;
	
	@Autowired
	private TimeseriesRepository timeseriesRepository;
	
	@Autowired
	@Qualifier(value="aliSourceV2")
	private AliSourceV2 aliSource;
	
	@Autowired
	@Qualifier(value="aliParser")
	private IParser aliParser;
	
	@Autowired
	@Qualifier(value="aliHistoricalSourceV2")
	private AliHistoricalDataSourceV2 aliHistoricalSourceV2;
	
	@Autowired
	@Qualifier(value="binanceSource")
	private BinanceSource binanceSource;
	
	@Autowired
	@Qualifier(value="aliHistoricalParser")
	private IParser aliHistoricalParser;
	
	@Autowired
	@Qualifier(value="binanceParser")
	private IParser binanceParser;
	
	@Autowired
	@Qualifier(value="currentSecurities")
	private Set<String> futureSecurities;
	
	@Autowired
	private TopDealRepository topDealRepo;
	
	@Autowired
	MessageService messageService;
	
	private boolean initTime = true;
	
	private Map<FutureSecurityEnum, String> mainSecurityMap = new HashMap<>();
	private Map<String, FutureSecurityEnum> securitiesOrderMap = new TreeMap<>(new Comparator<>() {
		@Override
		public int compare(String o1, String o2) {
			String v1 = o1.substring(0, o1.length() - 4);
			String v2 = o2.substring(0, o2.length() - 4);
			int order1 = getOrder(v1);
			int order2 = getOrder(v2);
			if(order1 - order2 != 0) {
				return order1 - order2;
			}
			int ret = v1.compareTo(v2);
			if(order1 == order2 && order1 == 0 && ret != 0) {
				return ret;
			}
			String l1 = o1.substring(o1.length() - 4);
			String l2 = o2.substring(o2.length() - 4);
			return l1.compareTo(l2);
		}
	});
	
	private int getOrder(String variety) {
		switch(variety) {
		case "RB":
			return -100;
		case "I":
			return -90;
			default:
				return 0;
		}
	}
	
	public void init() {
		Arrays.stream(FutureSecurityEnum.values()).forEach(e -> {
			List<String> securityList = e.getSecurities();
			for (String security : securityList) {
				securitiesOrderMap.put(security, e);
			}
			String mainSecurity = getMainSecurity(securityList);
			mainSecurityMap.put(e, mainSecurity);
			Iterator<String> it = securityList.iterator();
			while(it.hasNext()) {
				String security = it.next();
				if(security.compareTo(mainSecurity) < 0) {
					it.remove();
				}
			}
			Collections.sort(securityList);
		});
		log.info("Current security list:" + securitiesOrderMap);
	}
	
//	private boolean debug = true;
	public void update1MinData() {
		if(!TradeUtil.isTradingTime()) {
			return;
		}
//		if(!debug) {
//			return;
//		}
//		if(debug) {
//			debug = false;
//		}
		taskManager.subscribeSecurities();
		List<Timeseries> series = null;
		try {
			String content = aliSource.getContent(futureSecurities);
			FreqEnum freq = FreqEnum._1MI;
			series = aliParser.parse(content, freq);
			futureLoader.loadMultiSecurities(series, freq);
			
			LocalTime lt = LocalTime.now();
			if(lt.getMinute() % 15 == 0) {
				if(!(lt.getHour() == 21 && lt.getMinute() == 0) && !(lt.getHour() == 9 && lt.getMinute() == 0) && !(lt.getHour() == 13 && lt.getMinute() == 30)) {
					updateHistory1M();
				}
			}
		} catch (IOException e) {
			log.error("Error message when reading data from Ali", e);
			/**
			 * Should not find one security. Ignore this case.
			 */
		}
		try {
			messageService.send("future-MI-topic", series);
		} catch (JMSException e) {
			log.error("Error when sending message to topic", e);
		}
		try {
			if(needUpdate(LocalDateTime.now(), FreqEnum._15MI)) {
				log.info("need to update");
				update15MinData();
				initTime = false;
				log.info("complete to update 30 Min");
			} else if(initTime) {
				log.info("only need to send message");
				FreqEnum freq = FreqEnum._30MI;
				Map<String, List<Timeseries>> dataMap30M = MyFileUtil.readObject(FOLDER_PREFIX + "/" + freq.getValue() + "_data.ser");
				for (Entry<FutureSecurityEnum, String> entry : mainSecurityMap.entrySet()) {
					String securityStr = entry.getValue();
					List<Timeseries> currentSeries = dataMap30M.get(securityStr);
					sendMessage(freq, currentSeries);
				}
				initTime = false;
				log.info("complete to send message only");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void updateHistory1M() throws IOException {
		log.info("Retrieve history data for 1M freq");
		for (Entry<FutureSecurityEnum, String> entry : mainSecurityMap.entrySet()) {
			FutureSecurityEnum securityEnum = entry.getKey();
			String mainSecurityStr = entry.getValue();
			Variety variety = Variety.of(securityEnum.name());
			Security mainSecurity = new Security(mainSecurityStr, variety);
			FreqEnum _1MiFreq = FreqEnum._1MI;
			String _1MiContent = aliHistoricalSourceV2.getContent(mainSecurityStr, _1MiFreq, "1", "31");
			List<Timeseries> _1MiSeries = aliHistoricalParser.parseAll(mainSecurity, _1MiContent, _1MiFreq);
			futureLoader.loadSecurity(mainSecurity, _1MiFreq, _1MiSeries);
		}
	}
	
	public void updateHistory1MbyEOD() throws IOException {
		log.info("Retrieve history data for 1M freq");
		for (Entry<FutureSecurityEnum, String> entry : mainSecurityMap.entrySet()) {
			FutureSecurityEnum securityEnum = entry.getKey();
			String mainSecurityStr = entry.getValue();
			Variety variety = Variety.of(securityEnum.name());
			Security mainSecurity = new Security(mainSecurityStr, variety);
			FreqEnum _1MiFreq = FreqEnum._1MI;
			String _1MiContent = aliHistoricalSourceV2.getContent(mainSecurityStr, _1MiFreq, "1", "500");
			List<Timeseries> _1MiSeries = aliHistoricalParser.parseAll(mainSecurity, _1MiContent, _1MiFreq);
			futureLoader.loadSecurity(mainSecurity, _1MiFreq, _1MiSeries, 500);
		}
	}
	
	public void updateMetal() throws IOException, JMSException {
		Set<String> set = new HashSet<>();
		set.add("XAUUSD");
//		set.add("XAGUSD");
		for (String securityStr : set) {
			Security security = new Security(securityStr, Variety.of(securityStr));
			FreqEnum[] freqs = new FreqEnum[] { FreqEnum._1H, FreqEnum._1D, FreqEnum._15MI, FreqEnum._1MI };
//			FreqEnum[] freqs = new FreqEnum[] { FreqEnum._1MI };
			for (FreqEnum freq : freqs) {
				String content = aliHistoricalSourceV2.getContent(securityStr, freq, "1", "100");
				List<Timeseries> series = aliHistoricalParser.parseAll(security, content, freq);
				for (Timeseries ts : series) {
					ts.setOpenInterest(new BigDecimal(0));
				}
				futureLoader.loadSecurity(security, freq, series);
				sendMessage(freq, series);
			}
//			for (int i = 1; i <= 10; i++) {
//				for (FreqEnum freq : freqs) {
//					String content = aliHistoricalSourceV2.getContent(securityStr, freq, i + "", "500");
//					try {
//						List<Timeseries> series = aliHistoricalParser.parseAll(security, content, freq);
//						for (Timeseries ts : series) {
//							ts.setOpenInterest(new BigDecimal(0));
//						}
//						futureLoader.loadSecurity(security, freq, series);
//						sendMessage(freq, series);
//					}catch(Exception e) {
//						log.error("error for freq[" + freq.getValue() + "]", e);
//					}
//				}
//			}
		}
	}
	
	public void updateCoin() throws IOException, JMSException {
		Set<String> set = new HashSet<>();
		set.add("ETHUSDT");
		for (String securityStr : set) {
			Security security = new Security(securityStr, Variety.of(securityStr));
			FreqEnum[] freqs = new FreqEnum[] { FreqEnum._1H, FreqEnum._15MI, FreqEnum._5MI, FreqEnum._1MI, FreqEnum._1D };
			for (FreqEnum freq : freqs) {
				String content = binanceSource.getContent(securityStr, freq);
				List<Timeseries> series = binanceParser.parseAll(security, content, freq);
				for (Timeseries ts : series) {
					ts.setOpenInterest(new BigDecimal(0));
				}
				futureLoader.loadSecurity(security, freq, series);
				sendMessage(freq, series);
			}
		}
	}
	
	public void fillBack1D() {
		for (String variety : Constants.VARIETY_LIST) {
			List<String> tradeDateList = timeseriesRepository.findMaxTradeDate(variety);
			FreqEnum freq = FreqEnum._1D;
			String tradeDate = tradeDateList.get(0);
			for (Entry<String, FutureSecurityEnum> entry : securitiesOrderMap.entrySet()) {
				if(!entry.getValue().name().equals(variety)) {
					continue;
				}
				String securityStr = entry.getKey();
				List<Timeseries> miList = timeseriesRepository.findByTradeDateWithPeriod(securityStr, tradeDate, "1M");
				Timeseries latest = miList.get(miList.size() - 1);
				Security security = new Security(securityStr, Variety.of(variety));
				String content;
				try {
					content = aliHistoricalSourceV2.getContent(securityStr, freq, "1", "1");
					List<Timeseries> series = aliHistoricalParser.parseAll(security, content, freq);
					for (Timeseries ts : series) {
						ts.setOpenInterest(latest.getOpenInterest());
					}
					futureLoader.loadSecurity(security, freq, series);
				} catch (IOException e) {
					log.error("Error when loading 1D freq for[" + securityStr + "]", e);
				}
			}
		}
	}
	
	public void reload(String securityStr) {
		try {
			FreqEnum freq = FreqEnum._15MI;
			for (int i = 9; i >= 1; i--) {
				String content = aliHistoricalSourceV2.getContent(securityStr, freq, i + "");
				Security security = new Security(securityStr, Variety.of(securityStr.substring(0, securityStr.length() - 4)));
				List<Timeseries> series = aliHistoricalParser.parse(security, content, freq);
				futureLoader.loadSecurity(security, freq, series);
				
				List<Timeseries> min30Series = get30MinBy15Min(security, freq, 50);
				futureLoader.loadSecurity(security, FreqEnum._30MI, min30Series);
			}
		} catch (IOException e) {
		}
	}
	
	public void update15MinData() throws FileNotFoundException, IOException, ClassNotFoundException {
		LocalDateTime ldt = LocalDateTime.now();
		Map<String, List<Timeseries>> dataMap15M = new HashMap<>();
		Map<String, List<Timeseries>> dataMap30M = new HashMap<>();
		boolean update30Min = needUpdate(ldt, FreqEnum._30MI);
		securitiesOrderMap.entrySet().parallelStream().forEach(entry -> {
			try {
				String securityStr = entry.getKey();
				Security security = new Security(securityStr, Variety.of(securityStr.substring(0, securityStr.length() - 4)));
				List<Timeseries> list15Series = load15MiData(securityStr, security);
				dataMap15M.put(securityStr, list15Series);
				if(update30Min) {
					List<Timeseries> list30Series = load30MinData(security, FreqEnum._15MI);
					dataMap30M.put(securityStr, list30Series);
				}
			} catch (IOException | JMSException e) {
			}
		});
		LocalDateTime saved15Ldt = calculateTime(ldt, FreqEnum._15MI);
		MyFileUtil.writeFile(FOLDER_PREFIX + "/" + FreqEnum._15MI.getValue() + "_time.ser", saved15Ldt);
		MyFileUtil.writeFile(FOLDER_PREFIX + "/" + FreqEnum._15MI.getValue() + "_data.ser", dataMap15M);
		log.info("Save time for 15M:" + saved15Ldt);
		if(update30Min) {
			LocalDateTime saved30Ldt = calculateTime(ldt, FreqEnum._30MI);
			MyFileUtil.writeFile(FOLDER_PREFIX + "/" + FreqEnum._30MI.getValue() + "_time.ser", saved30Ldt);
			MyFileUtil.writeFile(FOLDER_PREFIX + "/" + FreqEnum._30MI.getValue() + "_data.ser", dataMap30M);
			log.info("Save time for 30M:" + saved30Ldt);
		}
	}
	
	private boolean needUpdate(LocalDateTime ldt, FreqEnum freq) throws FileNotFoundException, ClassNotFoundException, IOException {
		String fileName = FOLDER_PREFIX + "/" + freq.getValue() + "_time.ser";
		File file = new File(fileName);
		if(!file.exists()) {
			log.info("file doesn't exist - " + ldt + ". True");
			return true;
		}
		LocalDateTime lastTime = MyFileUtil.readObject(fileName);
		int dateRet = lastTime.toLocalDate().compareTo(ldt.toLocalDate());
		if(dateRet < 0) {
			log.info("date is different[" + lastTime + " vs " + ldt + ". True");
			return true;
		}
		LocalTime lastLt = lastTime.toLocalTime();
		LocalTime lt = ldt.toLocalTime();
		if(lastLt.getHour() > lt.getHour()) {
			return false;
		}
		if(lastLt.getHour() < lt.getHour()) {
			log.info("Hour is less[" + lastTime + " vs " + ldt + ". True");
			return true;
		}
		if(lastLt.getMinute() < lt.getMinute()) {
			log.info("Minute is less[" + lastTime + " vs " + ldt + ". True");
			return true;
		}
		return false;
	}
	
	private LocalDateTime calculateTime(LocalDateTime ldt, FreqEnum freq) {
		long min = ldt.getMinute();
		long value = freq.getPeriod() + 1;
		long offset = min % value;
		if(offset == 0) {
			return ldt;
		}
		long targetOffset = value - offset;
		return ldt.plusMinutes(targetOffset);
	}

	public List<Timeseries> load15MiData(String securityStr, Security security) throws IOException, JMSException {
		FreqEnum freq = FreqEnum._15MI;
		String content = aliHistoricalSourceV2.getContent(securityStr, freq);
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
		sendMessage(freq, series);
		return series;
	}
	
	private List<Timeseries> load30MinData(Security security, FreqEnum freq) throws JMSException{
		List<Timeseries> series = get30MinBy15Min(security, freq, 50);
		futureLoader.loadSecurity(security, FreqEnum._30MI, series);
		sendMessage(FreqEnum._30MI, series);
		return series;
	}
	
	private void sendMessage(FreqEnum freq, List<Timeseries> series) throws JMSException {
		messageService.send("future-" + freq.getValue() + "-topic", series);
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
			List<String> lastTradeDates = timeseriesRepository.getLast5TradeDate(e.name(), FreqEnum._1MI.getValue(), timestamp);
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
			if(openInterest == null) {
				continue;
			}
			if (mainSecurity == null) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
			if (openInterest != null && openInterest > maxOpenInterest) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
		}
		return mainSecurity;
	}
	
	public void loadMainTopDeal() {
		mainSecurityMap.entrySet().forEach(entry -> {
			loadTopDeal(entry.getKey(), entry.getValue());
		});
	}
	
	public void loadTopDeal(FutureSecurityEnum futureSecurityEnum, String security) {
		try(SeleniumParser parser = new SeleniumParser();){
			List<TopDeal> list = parser.lookupData(futureSecurityEnum, security, null);
			topDealRepo.saveAll(list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadTopDeal(FutureSecurityEnum futureSecurityEnum, String security, String tradeDate) {
		try(SeleniumParser parser = new SeleniumParser();){
			List<TopDeal> list = parser.lookupData(futureSecurityEnum, security, tradeDate);
			topDealRepo.saveAll(list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
