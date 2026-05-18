package michael.slf4j.investment.research;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.constant.TopicConstants;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.util.DataLoaderUtil;
import michael.slf4j.investment.util.IndicatorUtils;
import michael.slf4j.investment.util.PositionFileUtil;
import michael.slf4j.investment.util.StockIndicatorCalculator;
import michael.slf4j.investment.util.StockIndicatorCalculator.RSIResult;
import michael.slf4j.investment.util.StockIndicatorCalculator.StockData;
import michael.slf4j.investment.util.TechnicalIndicator;
import michael.slf4j.investment.util.TechnicalIndicator.KDJResult;
import michael.slf4j.investment.util.TechnicalIndicator.MACDResult;
import michael.slf4j.investment.util.TradeUtil;

@Component("coinDataResearch")
public class CoinDataResearch {
	private static final Logger log = Logger.getLogger(CoinDataResearch.class);
	private static final Pattern FILENAME_PATTERN = Pattern.compile(".*-(.*)-(.*)[.]txt");
	private static final Map<String, String> HEADER_MAP = new LinkedHashMap<>();
	private static final String FULL = "full";
	private static final String MONTH = "month";
	private static final String WEEK = "week";
	private static final String HIGH = "high";
	private static final String LOW = "low";
	private static final int startHour = 8;

	static {
		HEADER_MAP.put("time", "时间");
		HEADER_MAP.put("freq", "周期");
		HEADER_MAP.put("open", "开盘价");
		HEADER_MAP.put("high", "最高价");
		HEADER_MAP.put("low", "最低价");
		HEADER_MAP.put("close", "收盘价");
		HEADER_MAP.put("VOLUME", "VOLUME");
		HEADER_MAP.put("MA1", "MA5");
		HEADER_MAP.put("MA2", "MA8");
		HEADER_MAP.put("MA3", "MA13");
		HEADER_MAP.put("MA4", "MA21");
		HEADER_MAP.put("MA5", "MA34");
		HEADER_MAP.put("MA6", "MA55");
		HEADER_MAP.put("MA7", "MA89");
		HEADER_MAP.put("BOLL LOWER", "BOLL(26,2) LOWER");
		HEADER_MAP.put("BOLL MID", "BOLL(26,2) MID");
		HEADER_MAP.put("BOLL UPPER", "BOLL(26,2) UPPER");
		HEADER_MAP.put("BIAS1", "BIAS(6,12,24) BIAS1");
		HEADER_MAP.put("BIAS2", "BIAS(6,12,24) BIAS2");
		HEADER_MAP.put("BIAS3", "BIAS(6,12,24) BIAS3");
		HEADER_MAP.put("WR1", "WR(10,6,-80,-20) WR1");
		HEADER_MAP.put("WR2", "WR(10,6,-80,-20) WR2");
		HEADER_MAP.put("TR", "ATR(15) TR");
		HEADER_MAP.put("ATR", "ATR(15) ATR");
		HEADER_MAP.put("CCI", "CCI(14)");
		HEADER_MAP.put("MFI", "MFI(14)");
		HEADER_MAP.put("MACD DIFF", "MACD(12,26,9) DIFF");
		HEADER_MAP.put("MACD DEA", "MACD(12,26,9) DEA");
		HEADER_MAP.put("MACD MACD", "MACD(12,26,9) MACD");
		HEADER_MAP.put("K", "KDJ(9,3,3) K");
		HEADER_MAP.put("D", "KDJ(9,3,3) D");
		HEADER_MAP.put("J", "KDJ(9,3,3) J");
		HEADER_MAP.put("RSI1", "RSI(6,12,24) RSI1");
		HEADER_MAP.put("RSI2", "RSI(6,12,24) RSI2");
		HEADER_MAP.put("RSI3", "RSI(6,12,24) RSI3");
	}

	@Autowired
	private TimeseriesRepository timeseriesRepository;

	@Autowired
	private ChatLanguageModel chatModel;

	@Value("${chat.flag.folder}")
	private String flagFolderName;
	
	@Value(value = "${chat.research.folder}")
	private String researchFolder;
	
	@Value(value = "${chat.user.folder}")
	private String userFolder;
	
	@Value("${chat.position.folder}")
	private String positionFolderName;

	@Autowired
	private FileService fileService;

	@Autowired
	MessageService messageService;

	private Map<Variety, Integer> countMap = new HashMap<>();
	private Map<Variety, List<ChatMessage>> chatMap = new HashMap<>();
	private Variety variety;

	private NumberFormat nf;

	public CoinDataResearch() {
		this.nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setGroupingUsed(false);
		variety = Variety.ETHUSDT;
	}
	
	public void init() {
		log.info("Initialize research for [" + variety.name() + "]");
		List<ChatMessage> messages = new ArrayList<>();
		chatMap.put(variety, messages);
		String folder = researchFolder + "/" + variety.name() + "/";
		File folderDir = new File(folder);
		if(!folderDir.exists()) {
			folderDir.mkdirs();
		}
		String fileNamePrefix = getFilenamePrefix(variety, variety.name());
		StringBuffer startSb = new StringBuffer();
		startSb.append("你是一个专业的期货投资顾问,擅长技术分析和解释市场趋势.\n");
		startSb.append("我是激进投资者,我可以接受慢慢加仓,但是平仓的操作一定是全平.\n");
		startSb.append("交易说明:我是进行50倍杠杆交易,标的价格浮动1.7%如果是亏损方向就会强平导致资金归0.每一次开仓平仓的交易手续费差不多是标的价格浮动0.12%.所以你在生成策略的时候,务必要严谨,我会严格按照你的交易策略进行交易!");
		startSb.append("用中文回答");
		messages.add(SystemMessage.from(startSb.toString()));
		try {
			String[] fileNameArr = folderDir.list();
			List<String> fileNameList = new ArrayList<>();
			Arrays.stream(fileNameArr).forEach(fileName -> fileNameList.add(fileName));
			if(fileNameList.isEmpty()) {
				String initFileName = fileNamePrefix + "0-question.txt";
				summarize(initFileName, false);
				messages.add(UserMessage.from(getContent(initFileName)));
				
				log.info("Start deepseek for " + initFileName);
				Response<AiMessage> initAiReply = chatModel.generate(messages);
				String initReply = initAiReply.content().text();
				messages.add(AiMessage.from(initReply));
				writeFile(fileNamePrefix + "0-answer.txt", initReply);
				
				StringBuffer questionSb = new StringBuffer();
				String myTail = """
稍后,我会提供5M/15M/1H的所有指标数据以及1M的OHLCV数据.请到时候基于更新的数据数据,重新评估日内走势.如果你准备好了,请回复'确认'
注意:稍后需要对后续实时判断后续可能的形态
						""";
				questionSb.append(myTail);
				messages.add(UserMessage.from(questionSb.toString()));
				
				log.info("Start deepseek for " + fileNamePrefix + "1-answer.txt");
				Response<AiMessage> contextAiReply = chatModel.generate(messages);
				String contextReply = contextAiReply.content().text();
				messages.add(AiMessage.from(contextReply));
				
				writeFile(fileNamePrefix + "1-question.txt", questionSb.toString());
				writeFile(fileNamePrefix + "1-answer.txt", contextReply);
				
				countMap.put(variety, 2);
				log.info("Done to initialize realtime strategy for " + variety.name());
			} else {
				Collections.sort(fileNameList, new Comparator<String>() {
					@Override
					public int compare(String fileName1, String fileName2) {
						Matcher m1 = FILENAME_PATTERN.matcher(fileName1);
						Matcher m2 = FILENAME_PATTERN.matcher(fileName2);
						if(!m1.matches() || !m2.matches()) {
							throw new RuntimeException(fileName1 + " doesn't match pattern[" + FILENAME_PATTERN.pattern() + "]");
						}
						String numStr1 = m1.group(1);
						String numStr2 = m2.group(1);
						String type1 = m1.group(2);
						String type2 = m2.group(2);
						int num1 = Integer.valueOf(numStr1);
						int num2 = Integer.valueOf(numStr2);
						if(num1 < num2) {
							return -1;
						} else if(num1 > num2) {
							return 1;
						}
						if(type1.equals("question")) {
							return -1;
						} else if(type2.equals("question")) {
							return 1;
						}
						return 0;
					}
				});
				int index = 0;
				int direction = 0;
				for (String fileName : fileNameList) {
					String content = getContent(folder + "/" + fileName);
					if(direction == 0) {
						messages.add(UserMessage.from(content));
						direction = 1;
					} else {
						messages.add(AiMessage.from(content));
						direction = 0;
						index++;
					}
				}
				if(direction != 0) {
					log.info("Trigger extra conversation.");
					Response<AiMessage> contextAiReply = chatModel.generate(messages);
					String contextReply = contextAiReply.content().text();
					messages.add(AiMessage.from(contextReply));
					
					writeFile(fileNamePrefix + index + "-answer.txt", contextReply);
					index++;
				}
				countMap.put(variety, index);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("RealTime is ready for " + variety.name());
	}
	
	public void summarize() {
		String flagFileName = flagFolderName + "/" + variety.name() + "-flag.txt";
		log.info("Start to get new research for " + variety.name());
		
		LocalDateTime lastCheckPoint = readLocalDateTime(flagFileName);
		LocalDateTime tmp = LocalDateTime.now();
		int currentMinute = tmp.getMinute() - (tmp.getMinute() % 30);
		LocalDateTime latestCheckPoint = LocalDateTime.of(tmp.toLocalDate(), LocalTime.of(tmp.getHour(), currentMinute));
		if(lastCheckPoint != null && lastCheckPoint.toLocalDate().equals(latestCheckPoint.toLocalDate()) && lastCheckPoint.getHour() == latestCheckPoint.getHour() && lastCheckPoint.getMinute() == latestCheckPoint.getMinute()) {
			return;
		} else if(lastCheckPoint != null && latestCheckPoint.isBefore(lastCheckPoint)) {
			return;
		} else if(lastCheckPoint != null && (lastCheckPoint.getDayOfWeek() == DayOfWeek.SATURDAY && lastCheckPoint.getHour() == 5) && ((latestCheckPoint.getDayOfWeek() == DayOfWeek.SATURDAY) || (latestCheckPoint.getDayOfWeek() == DayOfWeek.SUNDAY) || (latestCheckPoint.getDayOfWeek() == DayOfWeek.MONDAY && latestCheckPoint.getHour() <= startHour - 1))) {
			return;
		}
		boolean adhoc = true;
		if(latestCheckPoint.getHour() == startHour - 1) {
			adhoc = false;
		} else if(lastCheckPoint != null) {
			if(lastCheckPoint.getHour() < startHour + 1 && startHour <= latestCheckPoint.getHour()) {
				adhoc = false;
			} else if(lastCheckPoint.getDayOfYear() != latestCheckPoint.getDayOfYear() && startHour + 1 <= latestCheckPoint.getHour()) {
				adhoc = false;
			}
		} else {
			adhoc = false;
		}
		if(!adhoc) {
			log.info("Time time is full run, housekeep first");
			cleanup();
			init();
		} else {
			init();
			log.info("Time time is adhoc run");
			int index = countMap.get(variety);
			try {
				String fileNamePrefix = getFilenamePrefix(variety, variety.name());
				String questionFileName = fileNamePrefix + index + "-question.txt";
				String answerFileName = fileNamePrefix + index + "-answer.txt";
				summarize(questionFileName, adhoc);
				String question = getContent(questionFileName);
				
				List<ChatMessage> messages = chatMap.get(variety);
				messages.add(UserMessage.from(question));
				
				log.info("Start real time deepseek for[" + variety + "]");
				Response<AiMessage> aiReply = chatModel.generate(messages);
				String reply = aiReply.content().text();
				messages.add(AiMessage.from(reply));
				
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("receiver", "! ( L｜P )");
				jsonObj.put("variety", variety.name());
				messageService.send(TopicConstants.NOTIFICATION_TOPIC, jsonObj.toString());
				writeFile(questionFileName, question);
				writeFile(answerFileName, reply);
			} catch (Exception e) {
				e.printStackTrace();
			}
			index++;
			countMap.put(variety, index);
		}
	}
	
	public void summarize(String fileName, boolean adhoc) {
		String flagFileName = flagFolderName + "/" + variety.name() + "-flag.txt";
		LocalDateTime current = LocalDateTime.now();
		Timestamp ts = TradeUtil.getTimestamp(current);
		List<String> lastTradeDates = timeseriesRepository.getLast5TradeDate(variety.name(), FreqEnum._1H.getValue(),
				ts);
		String tTradeDate = lastTradeDates.get(0);
		String latestTradeDate = tTradeDate;
		String mainSecurity = variety.name();

		List<StringBuffer> formatList = new ArrayList<StringBuffer>();

		List<Timeseries> realTime1HList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, latestTradeDate,
				FreqEnum._1H.getValue());
		Timeseries last1HTs = realTime1HList.get(realTime1HList.size() - 1);
		LocalDateTime tradeTs = TradeUtil.getLocalDateTime(last1HTs.getTradeTs());
		LocalDateTime ldt = LocalDateTime.now();
		if(ldt.getHour() == tradeTs.getHour() && ldt.getDayOfMonth() == tradeTs.getDayOfMonth()) {
			realTime1HList.remove(realTime1HList.size() - 1);
		}
		
		LocalDateTime lastCheckPoint = readLocalDateTime(flagFileName);
		last1HTs = realTime1HList.get(realTime1HList.size() - 1);
		LocalDateTime latestCheckPoint = TradeUtil.getLocalDateTime(last1HTs.getTradeTs()).plusHours(1);
		
		generateHeader(formatList, mainSecurity, current, last1HTs);
		
		StringBuffer sb = new StringBuffer();
		sb.append("下面表格包括了不同周期指标的数据:");
		sb.append("\n");
		sb.append("|");
		sb.append(HEADER_MAP.values().stream().collect(Collectors.joining("|")));
		sb.append("|");
		sb.append("\n");
		formatList.add(sb);

		if(!adhoc) {
			Queue<StringBuffer> queue1H = summarizeDataByFreq(FreqEnum._1H, realTime1HList, 61);
			queue1H.stream().forEach(currentSb -> formatList.add(currentSb));
			
			List<Timeseries> realTimeList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, tTradeDate,
					FreqEnum._1D.getValue());
			if(ldt.getDayOfWeek().getValue() <= 5) {
				realTimeList.remove(realTimeList.size() - 1);
			}
			Map<Timestamp, Timeseries> map = new TreeMap<>();
			for (Timeseries timeseries : realTimeList) {
				map.put(timeseries.getTradeTs(), timeseries);
			}
			List<Timeseries> realTimeList1D = new ArrayList<>();
			for (Entry<Timestamp, Timeseries> entry : map.entrySet()) {
				realTimeList1D.add(entry.getValue());
			}
			Queue<StringBuffer> queue1D = summarizeDataByFreq(FreqEnum._1D, realTimeList1D, 60);
			queue1D.stream().forEach(currentSb -> formatList.add(currentSb));

			/**
			 * 1W frequence data
			 */
			List<Timeseries> realTimeList1W = DataLoaderUtil.generate1WTsListBy1D(realTimeList1D);
			Queue<StringBuffer> queue1W = summarizeDataByFreq(FreqEnum._1W, realTimeList1W, 60);
			queue1D.stream().forEach(currentSb -> formatList.add(currentSb));
			queue1W.stream().forEach(currentSb -> formatList.add(currentSb));
			
			/**
			 * 5M frequence data
			 */
			List<Timeseries> realTime5MList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, latestTradeDate,
					FreqEnum._5MI.getValue());
			Queue<StringBuffer> queue5M = summarizeDataByFreq(FreqEnum._5MI, realTime5MList, 60);
			queue5M.stream().forEach(currentSb -> formatList.add(currentSb));

			/**
			 * 15M frequence data
			 */
			List<Timeseries> realTime15MList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, latestTradeDate,
					FreqEnum._15MI.getValue());
			Queue<StringBuffer> queue15M = summarizeDataByFreq(FreqEnum._15MI, realTime15MList, 96);
			queue15M.stream().forEach(currentSb -> formatList.add(currentSb));
			
			generateKeyPoints(formatList, realTimeList1D);
			generateTrail(variety.name(), latestTradeDate, formatList, mainSecurity, current, realTime1HList.get(realTime1HList.size() - 1));
			StringBuffer positionSb = new StringBuffer();
			positionSb.append("第四步:");
			StringBuffer strategySb = getPosition(variety, last1HTs);
			positionSb.append(strategySb);
			formatList.add(positionSb);
		} else {
			/**
			 * 5M frequence data
			 */
			List<Timeseries> realTime5MList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, latestTradeDate,
					FreqEnum._5MI.getValue());
			Queue<StringBuffer> queue5M = summarizeDataByFreq(FreqEnum._5MI, realTime5MList, lastCheckPoint, latestCheckPoint, startHour);
			queue5M.stream().forEach(currentSb -> formatList.add(currentSb));

			/**
			 * 15M frequence data
			 */
			List<Timeseries> realTime15MList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, latestTradeDate,
					FreqEnum._15MI.getValue());
			Queue<StringBuffer> queue15M = summarizeDataByFreq(FreqEnum._15MI, realTime15MList, lastCheckPoint, latestCheckPoint, startHour);
			queue15M.stream().forEach(currentSb -> formatList.add(currentSb));

			/**
			 * 1H frequence data
			 */
			Queue<StringBuffer> queue1H = summarizeDataByFreq(FreqEnum._1H, realTime1HList, lastCheckPoint, latestCheckPoint, startHour);
			queue1H.stream().forEach(currentSb -> formatList.add(currentSb));
			
			/**
			 * 5M frequence data
			 */
			Instant lastInstant = lastCheckPoint.atZone(ZoneId.systemDefault()).toInstant();
			Timestamp lastTimestamp = Timestamp.from(lastInstant);
			
			Instant latestInstant = latestCheckPoint.atZone(ZoneId.systemDefault()).toInstant();
			Timestamp latestTimestamp = Timestamp.from(latestInstant);
			
			List<Timeseries> realTime1MList = timeseriesRepository.getAllDataDuringTS(mainSecurity, latestTradeDate,
					FreqEnum._1MI.getValue(), lastTimestamp, latestTimestamp);
			StringBuffer _1Msb = new StringBuffer();
			_1Msb.append("\n");
			_1Msb.append("Time|Open|High|Low|Close|Volume\n");
			for (Timeseries _1mTs : realTime1MList) {
				LocalDateTime _1mTradeTs = TradeUtil.getLocalDateTime(_1mTs.getTradeTs());
				_1Msb.append(_1mTradeTs.format(DateTimeFormatter.ISO_DATE_TIME));
				_1Msb.append("|");
				_1Msb.append(_1mTs.getOpen().doubleValue());
				_1Msb.append("|");
				_1Msb.append(_1mTs.getHigh().doubleValue());
				_1Msb.append("|");
				_1Msb.append(_1mTs.getLow().doubleValue());
				_1Msb.append("|");
				_1Msb.append(_1mTs.getClose().doubleValue());
				_1Msb.append("|");
				_1Msb.append(_1mTs.getVolume().doubleValue());
				_1Msb.append("\n");
			}
			formatList.add(_1Msb);
			
			StringBuffer latestSb = new StringBuffer();
			latestSb.append("""
根据最新的数据更新分析,着重关注最近3小时的走势分析(请做详细说明)
					""");
			formatList.add(latestSb);
			
			StringBuffer strategySb = getPosition(variety, last1HTs);
			formatList.add(strategySb);
		}
		
		try {
			String userInputFileName = userFolder + "/" + variety.name() + "-input.txt";
			String content = getContent(userInputFileName);
			if(!content.isBlank()) {
				StringBuffer userInputSb = new StringBuffer();
				userInputSb.append("用户输入的额外信息:");
				userInputSb.append(content);
				formatList.add(userInputSb);
				writeFile(userInputFileName, "");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))) {
			for (StringBuffer strb : formatList) {
				bw.write(strb.toString());
				bw.flush();
			}
			log.info("Latest information is generated");
			fileService.getFileStatus(variety);
		} catch (Exception e) {
			log.error("Error when sending message to topic", e);
		}
		writeLocalDateTime(latestCheckPoint, flagFileName);
	}

	private StringBuffer getPosition(Variety variety, Timeseries last1HTs) {
		StringBuffer strategySb = new StringBuffer();
		int closePrice = last1HTs.getClose().intValue();
		Map<String, String> map = PositionFileUtil.readPositionData(variety.name());
		StringBuffer anotherCase = new StringBuffer();
		if (!map.isEmpty()) {
			double v = Double.valueOf(map.get(PositionFileUtil.PRICE));
			int direction = Integer.valueOf(map.get(PositionFileUtil.DIRECTION_INT));
			anotherCase.append(map.get(PositionFileUtil.DIRECTION));
			anotherCase.append("开仓价").append(v).append(",");
			anotherCase.append("浮");
			if(direction * (closePrice - v) > 0) {
				anotherCase.append("盈");
			} else {
				anotherCase.append("亏");
			}
			anotherCase.append(Math.abs(closePrice - v)).append("点,");
			anotherCase.append("仓位").append(map.get(PositionFileUtil.POSITION_PER)).append("%");
		}
		strategySb.append("针对以下情况制定交易策略(");
		String s = "";
		if(anotherCase.isEmpty()) {
			strategySb.append("空仓");
		} else {
			strategySb.append(anotherCase);
			s = ",如果当前仓位平仓,后续策略如何";
		}
		strategySb.append(")");
		strategySb.append(s);
		strategySb.append("\n");
		return strategySb;
	}

	private void generateKeyPoints(List<StringBuffer> formatList, List<Timeseries> realTimeList) {
		Timeseries lastTs = realTimeList.get(realTimeList.size() - 1);
		String latestTradeDate = lastTs.getTradeDate();
		LocalDate lastLd = TradeUtil.getTradeDate(latestTradeDate);
		Map<String, Map<String, BigDecimal>> map = new HashMap<>();
		for (int i = 0; i < realTimeList.size() - 1; i++) {
			Timeseries ts = realTimeList.get(i);
			updateMap(map, FULL, ts);

			String tradeDate = ts.getTradeDate();
			LocalDate ld = TradeUtil.getTradeDate(tradeDate);
			if (ld.plusDays(35).compareTo(lastLd) >= 0) {
				updateMap(map, MONTH, ts);
			}
			if (ld.plusDays(7).compareTo(lastLd) >= 0) {
				updateMap(map, WEEK, ts);
			}
		}
	}

	private void updateMap(Map<String, Map<String, BigDecimal>> map, String freq, Timeseries ts) {
		Map<String, BigDecimal> freqMap = getMap(map, freq);
		updateMap(freqMap, ts);
	}

	private Map<String, BigDecimal> getMap(Map<String, Map<String, BigDecimal>> map, String freq) {
		Map<String, BigDecimal> freqMap = map.get(freq);
		if (freqMap == null) {
			freqMap = new LinkedHashMap<String, BigDecimal>();
			freqMap.put(HIGH, new BigDecimal(0));
			freqMap.put(LOW, new BigDecimal("100000000000000000000"));
			map.put(freq, freqMap);
		}
		return freqMap;
	}

	private void updateMap(Map<String, BigDecimal> map, Timeseries ts) {
		BigDecimal high = map.get(HIGH);
		BigDecimal low = map.get(LOW);
		if (high.compareTo(ts.getHigh()) < 0) {
			map.put(HIGH, ts.getHigh());
		}
		if (low.compareTo(ts.getLow()) > 0) {
			map.put(LOW, ts.getLow());
		}
	}
	
	private void generateHeader(List<StringBuffer> formatList, String mainSecurity, LocalDateTime current,
			Timeseries lastTs) {
		StringBuffer sb = new StringBuffer();
		int closePrice = lastTs.getClose().intValue();
		sb.append("现在时间是").append(current.format(DateTimeFormatter.ISO_DATE_TIME)).append(",");
		sb.append("收盘点位").append(closePrice);
		sb.append("\n");
		sb.append("输出严格不少于5000个字符!!!\n");
		sb.append("分析");
		sb.append(mainSecurity);
		sb.append("下一个交易日的日内走势预演,和对应的概率,和关键价位预判\n");
		formatList.add(sb);
	}

	private void generateTrail(String varietyStr, String tDate, List<StringBuffer> formatList, String mainSecurity, LocalDateTime current,
			Timeseries lastTs) {
		StringBuffer sb = new StringBuffer();
		int closePrice = lastTs.getClose().intValue();
		sb.append("现在是北京时间").append(current.format(DateTimeFormatter.ISO_DATE_TIME)).append(",");
		sb.append("收盘点位").append(closePrice);
		sb.append("\n");
		sb.append("分析");
		sb.append(mainSecurity);
		sb.append("下一个交易日的日内走势预演,和对应的概率,和关键价位预判(请做详细说明)\n");
		
		String command = """
第一步,请首先确认的所有数据表格和指标清单:
1. 周期指标表:必须包括时间/周期/开盘价/最高价/最低价/收盘价/VOLUME,以及以下所有技术指标:
 - 趋势指标:MA5,MA8,MA13,MA21,MA34,MA55,MA89
 - 通道指标:BOLL LOWER, BOLL MID, BOLL UPPER
 - 震荡/动量指标:BIAS;WR;CCI(14);MFI(14);RSI
 - 趋势振荡指标:MACD
 - 随机指标:KDJ
 - 波动指标:ATR
第二步,请确认所有数据清单都将会被用作分析
第三步:逐项分析
请按以下结构分析(每一项技术指标都必须使用),每一项都必须明确引用上一步列出的具体指标名称和最新数值:
1.每一个周期的所有历是K线都要被以用,不能只做概括
2.日内走势预演:只分析接下来3小时内的波动,给出3小时内以下形态列别中可能出现的形态及其对应的走势,触发条件和概率,并重点分析后续的技术目标位和形态变化(请做详细说明).

形态中的哪些可能性最大(需考虑是否需要融合下跌中继和上涨中继)
单边上涨
单边下跌
稳步推升
震荡攀升
窄幅横盘（织布机）
宽幅震荡（过山车）
探底回升（V型）
冲高回落（倒V型）
探底后平走（L型）
冲高后平走（倒L型）
N型走势
倒N型走势
分时双底（W底）
分时双头（M头）
强攻形态（线上遛马）
防守形态（线下运行）
纠结形态（线间缠绕）
极端弱势（跳水远离）
价升量增
价升量缩
放量滞涨
缩量阴跌
脉冲放量
钓鱼线
心电图
阶梯式拉升
阶梯式跳水
				""";
		sb.append(command);
		sb.append("\n");
		
		sb.append("数据说明:NA代表当前数据缺失");
		sb.append("\n");
		sb.append("格式说明:不能出现table格式");
		sb.append("\n");
		sb.append("交易时间说明:北京时间周一6AM到周六5:00AM");
		sb.append("\n");
		formatList.add(sb);
	}

	private Queue<StringBuffer> summarizeDataByFreq(FreqEnum freq, List<Timeseries> realTimeTsList, int limit) {
		List<Double> opens = new ArrayList<Double>();
		List<Double> highs = new ArrayList<Double>();
		List<Double> closes = new ArrayList<Double>();
		List<Double> lows = new ArrayList<Double>();
		List<Double> volumes = new ArrayList<Double>();
		List<StockData> dataList = new ArrayList<>();
		Queue<StringBuffer> ret = new LinkedBlockingQueue<>();

		int size = realTimeTsList.size();

		for (int i = 0; i < size; i++) {
			Timeseries ts = realTimeTsList.get(i);
			if(ts.getVolume().intValue() == 0) {
				continue;
			}
			if(FreqEnum.getFreq(ts.getFreq()) == FreqEnum._1D && "2026-04-03".equals(ts.getTradeDate())) {
				continue;
			}
			opens.add(ts.getOpen().doubleValue());
			highs.add(ts.getHigh().doubleValue());
			lows.add(ts.getLow().doubleValue());
			closes.add(ts.getClose().doubleValue());
			volumes.add(ts.getVolume().doubleValue());
			double preClose = 0D;
			if (!dataList.isEmpty()) {
				preClose = dataList.get(dataList.size() - 1).getClose();
			}
			dataList.add(new StockData(ts.getOpen().doubleValue(), ts.getHigh().doubleValue(),
					ts.getLow().doubleValue(), ts.getClose().doubleValue(), ts.getVolume().doubleValue(), preClose));

			Map<String, String> dataMap = new LinkedHashMap<String, String>();

			LocalDateTime tradeTs = TradeUtil.getLocalDateTime(ts.getTradeTs());
			if(freq == FreqEnum._1H) {
				tradeTs = tradeTs.plusHours(1);
			}
			dataMap.put("time", tradeTs.format(DateTimeFormatter.ISO_DATE_TIME));
			dataMap.put("freq", freq.getValue());
			dataMap.put("open", nf.format(ts.getOpen()));
			dataMap.put("high", nf.format(ts.getHigh()));
			dataMap.put("low", nf.format(ts.getLow()));
			dataMap.put("close", nf.format(ts.getClose()));
			dataMap.put("OI", nf.format(ts.getOpenInterest()));
			dataMap.put("VOLUME", nf.format(ts.getVolume()));
			Map<String, List<Double>> mas = IndicatorUtils.calculateMA(closes);
			for (Entry<String, List<Double>> entry : mas.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> boll = IndicatorUtils.calculateBOLL(closes, 26, 2);
			for (Entry<String, List<Double>> entry : boll.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			String emaV = "NA";
			if (closes.size() >= 10) {
				double ema10 = IndicatorUtils.calculateEMA(closes, 10);
				emaV = nf.format(ema10);
			}
			dataMap.put("EMA", emaV);

			Map<String, List<Double>> bias = IndicatorUtils.calculateBIAS(closes);
			for (Entry<String, List<Double>> entry : bias.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> wr = IndicatorUtils.calculateWR(highs, lows, closes);
			for (Entry<String, List<Double>> entry : wr.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value * -1);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> atr = IndicatorUtils.calculateATR(highs, lows, closes, 15);
			for (Entry<String, List<Double>> entry : atr.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}
			List<Double> cci = IndicatorUtils.calculateCCI(highs, lows, closes, 14);
			String cciV = "NA";
			if (!cci.isEmpty()) {
				cciV = nf.format(cci.get(cci.size() - 1));
			}
			dataMap.put("CCI", cciV);

			double mfi14 = IndicatorUtils.calculateMFI(highs, lows, closes, volumes, 14);
			dataMap.put("MFI", nf.format(mfi14));

			String difV = "NA";
			String deaV = "NA";
			String macdV = "NA";
			if (closes.size() >= 26) {
				MACDResult macd = TechnicalIndicator.calculateMACD(closes);
				difV = nf.format(macd.dif);
				deaV = nf.format(macd.dea);
				macdV = nf.format(macd.macd);
			}
			dataMap.put("MACD DIFF", difV);
			dataMap.put("MACD DEA", deaV);
			dataMap.put("MACD MACD", macdV);

			KDJResult kdj = TechnicalIndicator.calculateKDJ(highs, lows, closes);
			dataMap.put("K", nf.format(kdj.k));
			dataMap.put("D", nf.format(kdj.d));
			dataMap.put("J", nf.format(kdj.j));

			String rsiV6 = "NA";
			String rsiV12 = "NA";
			String rsiV24 = "NA";
			if (dataList.size() > 24) {
				RSIResult rsi = StockIndicatorCalculator.calculateRSI(dataList);
				if (rsi.getRsi6() != null && rsi.getRsi12() != null && rsi.getRsi24() != null) {
					rsiV6 = nf.format(rsi.getRsi6());
					rsiV12 = nf.format(rsi.getRsi12());
					rsiV24 = nf.format(rsi.getRsi24());
				}
			}
			dataMap.put("RSI1", rsiV6);
			dataMap.put("RSI2", rsiV12);
			dataMap.put("RSI3", rsiV24);

			StringBuffer dataSb = new StringBuffer();
			dataSb.append("|");
			dataSb.append(HEADER_MAP.keySet().stream().map(key -> dataMap.get(key)).collect(Collectors.joining("|")));
			dataSb.append("|");
			dataSb.append('\n');

			if (ret.size() == limit) {
				ret.poll();
			}
			ret.add(dataSb);
		}
		return ret;
	}
	
	private Queue<StringBuffer> summarizeDataByFreq(FreqEnum freq, List<Timeseries> realTimeTsList, LocalDateTime lastCheckPoint, LocalDateTime latestCheckPoint, int startHour) {
		List<Double> opens = new ArrayList<Double>();
		List<Double> highs = new ArrayList<Double>();
		List<Double> closes = new ArrayList<Double>();
		List<Double> lows = new ArrayList<Double>();
		List<Double> volumes = new ArrayList<Double>();
		List<StockData> dataList = new ArrayList<>();
		Queue<StringBuffer> ret = new LinkedBlockingQueue<>();

		int size = realTimeTsList.size();

		for (int i = 0; i < size; i++) {
			Timeseries ts = realTimeTsList.get(i);
			if(ts.getVolume().intValue() == 0) {
				continue;
			}
			if(FreqEnum.getFreq(ts.getFreq()) == FreqEnum._1D && "2026-04-03".equals(ts.getTradeDate())) {
				continue;
			}
			opens.add(ts.getOpen().doubleValue());
			highs.add(ts.getHigh().doubleValue());
			lows.add(ts.getLow().doubleValue());
			closes.add(ts.getClose().doubleValue());
			volumes.add(ts.getVolume().doubleValue());
			double preClose = 0D;
			if (!dataList.isEmpty()) {
				preClose = dataList.get(dataList.size() - 1).getClose();
			}
			dataList.add(new StockData(ts.getOpen().doubleValue(), ts.getHigh().doubleValue(),
					ts.getLow().doubleValue(), ts.getClose().doubleValue(), ts.getVolume().doubleValue(), preClose));

			Map<String, String> dataMap = new LinkedHashMap<String, String>();

			LocalDateTime tradeTs = TradeUtil.getLocalDateTime(ts.getTradeTs());
			if(freq == FreqEnum._1H) {
				tradeTs = tradeTs.plusHours(1);
			} else if(freq == FreqEnum._15MI) {
				tradeTs = tradeTs.plusMinutes(15);
			} else if(freq == FreqEnum._5MI) {
				tradeTs = tradeTs.plusMinutes(5);
			}
			dataMap.put("time", tradeTs.format(DateTimeFormatter.ISO_DATE_TIME));
			dataMap.put("freq", freq.getValue());
			dataMap.put("open", nf.format(ts.getOpen()));
			dataMap.put("high", nf.format(ts.getHigh()));
			dataMap.put("low", nf.format(ts.getLow()));
			dataMap.put("close", nf.format(ts.getClose()));
			dataMap.put("OI", nf.format(ts.getOpenInterest()));
			dataMap.put("VOLUME", nf.format(ts.getVolume()));
			Map<String, List<Double>> mas = IndicatorUtils.calculateMA(closes);
			for (Entry<String, List<Double>> entry : mas.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> boll = IndicatorUtils.calculateBOLL(closes, 26, 2);
			for (Entry<String, List<Double>> entry : boll.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			String emaV = "NA";
			if (closes.size() >= 10) {
				double ema10 = IndicatorUtils.calculateEMA(closes, 10);
				emaV = nf.format(ema10);
			}
			dataMap.put("EMA", emaV);

			Map<String, List<Double>> bias = IndicatorUtils.calculateBIAS(closes);
			for (Entry<String, List<Double>> entry : bias.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> wr = IndicatorUtils.calculateWR(highs, lows, closes);
			for (Entry<String, List<Double>> entry : wr.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value * -1);
				}
				dataMap.put(entry.getKey(), v);
			}

			Map<String, List<Double>> atr = IndicatorUtils.calculateATR(highs, lows, closes, 15);
			for (Entry<String, List<Double>> entry : atr.entrySet()) {
				String v = "NA";
				if (!entry.getValue().isEmpty()) {
					double value = entry.getValue().get(entry.getValue().size() - 1);
					v = nf.format(value);
				}
				dataMap.put(entry.getKey(), v);
			}
			List<Double> cci = IndicatorUtils.calculateCCI(highs, lows, closes, 14);
			String cciV = "NA";
			if (!cci.isEmpty()) {
				cciV = nf.format(cci.get(cci.size() - 1));
			}
			dataMap.put("CCI", cciV);

			double mfi14 = IndicatorUtils.calculateMFI(highs, lows, closes, volumes, 14);
			dataMap.put("MFI", nf.format(mfi14));

			String difV = "NA";
			String deaV = "NA";
			String macdV = "NA";
			if (closes.size() >= 26) {
				MACDResult macd = TechnicalIndicator.calculateMACD(closes);
				difV = nf.format(macd.dif);
				deaV = nf.format(macd.dea);
				macdV = nf.format(macd.macd);
			}
			dataMap.put("MACD DIFF", difV);
			dataMap.put("MACD DEA", deaV);
			dataMap.put("MACD MACD", macdV);

			KDJResult kdj = TechnicalIndicator.calculateKDJ(highs, lows, closes);
			dataMap.put("K", nf.format(kdj.k));
			dataMap.put("D", nf.format(kdj.d));
			dataMap.put("J", nf.format(kdj.j));

			String rsiV6 = "NA";
			String rsiV12 = "NA";
			String rsiV24 = "NA";
			if (dataList.size() > 24) {
				RSIResult rsi = StockIndicatorCalculator.calculateRSI(dataList);
				if (rsi.getRsi6() != null && rsi.getRsi12() != null && rsi.getRsi24() != null) {
					rsiV6 = nf.format(rsi.getRsi6());
					rsiV12 = nf.format(rsi.getRsi12());
					rsiV24 = nf.format(rsi.getRsi24());
				}
			}
			dataMap.put("RSI1", rsiV6);
			dataMap.put("RSI2", rsiV12);
			dataMap.put("RSI3", rsiV24);

			StringBuffer dataSb = new StringBuffer();
			dataSb.append("|");
			dataSb.append(HEADER_MAP.keySet().stream().map(key -> dataMap.get(key)).collect(Collectors.joining("|")));
			dataSb.append("|");
			dataSb.append('\n');
			if (tradeTs.compareTo(lastCheckPoint) > 0 && tradeTs.compareTo(latestCheckPoint) <= 0) {
				ret.add(dataSb);
			}
		}
		return ret;
	}
	
	private static void writeLocalDateTime(LocalDateTime dateTime, String fileName) {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))){
        	StringBuffer sb = new StringBuffer();
        	sb.append(dateTime.getYear()).append(" ");
        	sb.append(dateTime.getMonthValue()).append(" ");
        	sb.append(dateTime.getDayOfMonth()).append(" ");
        	sb.append(dateTime.getHour()).append(" ");
        	sb.append(dateTime.getMinute());
        	bw.write(sb.toString());
        	bw.flush();
        	log.info("已写入: " + dateTime);
        } catch (Exception e) {
        	throw new RuntimeException(e);
		}
    }

    private static LocalDateTime readLocalDateTime(String fileName) {
    	try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))){
    		String line = br.readLine();
    		if(line == null) {
    			return null;
    		}
    		String[] parts = line.split("[\\s]+");
    		int year = Integer.valueOf(parts[0]);
    		int month = Integer.valueOf(parts[1]);
    		int day = Integer.valueOf(parts[2]);
    		int hour = Integer.valueOf(parts[3]);
    		int minute = Integer.valueOf(parts[4]);
    		return LocalDateTime.of(year, month, day, hour, minute);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
		}
    }
    
    private String getFilenamePrefix(Variety variety, String mainSecurity) {
		String fileNamePrefix = researchFolder + "/" + variety.name() + "/" + mainSecurity + "-";
		return fileNamePrefix;
	}
    
    private String getContent(String fileName) throws FileNotFoundException, IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))) {
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString();
		}
	}
	
	private void writeFile(String fileName, String content) throws FileNotFoundException, IOException {
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))){
			bw.write(content);
			bw.newLine();
			bw.flush();
		}
	}
	
	public void cleanup() {
		countMap.clear();
		chatMap.clear();
		File realtimeDir = new File(researchFolder + "/" + variety.name());
		if (realtimeDir.exists() && realtimeDir.isDirectory()) {
			try {
				Files.walk(realtimeDir.toPath()).skip(1).sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException e) {
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String flagFileName = flagFolderName + "/" + variety.name() + "-flag.txt";
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(flagFileName)))){
        	StringBuffer sb = new StringBuffer();
        	sb.append("");
        	bw.write(sb.toString());
        	bw.flush();
        } catch (Exception e) {
        	throw new RuntimeException(e);
		}
	}
	
	public BigDecimal getCurrentPosition() throws FileNotFoundException, IOException {
		LocalDateTime current = LocalDateTime.now();
		Timestamp ts = TradeUtil.getTimestamp(current);
	
		List<String> lastTradeDates = timeseriesRepository.getLast5TradeDate(variety.name(), FreqEnum._1H.getValue(),
				ts);
		String tTradeDate = lastTradeDates.get(0);
		String latestTradeDate = tTradeDate;
		String mainSecurity = variety.name();

		List<Timeseries> realTime1HList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, latestTradeDate,
				FreqEnum._1H.getValue());
		Timeseries last1HTs = realTime1HList.get(realTime1HList.size() - 1);
		
		String positionFileName = positionFolderName + "/" + variety.name() + "/record.txt";
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(positionFileName)))){
			String line = null;
			double sum = 1D;
			String[] pre = null;
			while((line = br.readLine()) != null) {
				String[] parts = line.split(",");
				int direction = Integer.valueOf(parts[1]);
				double dealPrice = Double.valueOf(parts[2]);
				
				if(pre == null) {
					if(direction != 0) {
						pre = parts;
					}
					continue;
				}
				int preD = Integer.valueOf(pre[1]);
				double prePrice = Double.valueOf(pre[2]);
				double prePercent = Double.valueOf(pre[3]) * 0.01D;
				
				if(direction == preD) {
					pre = parts;
					continue;
				}
				if(direction == 0 || direction != preD) {
					sum += sum * (dealPrice - prePrice - prePrice * 0.0012D) * preD * prePercent * 50D / prePrice;
					pre = null;
					if(direction != 0) {
						pre = parts;
					}
				}
			}
			if(pre != null) {
				int preD = Integer.valueOf(pre[1]);
				double prePrice = Double.valueOf(pre[2]);
				double prePercent = Double.valueOf(pre[3]) * 0.01D;
				sum += sum * (last1HTs.getClose().doubleValue() - prePrice - prePrice * 0.001D) * preD * prePercent * 50D / prePrice;
			}
			return new BigDecimal(sum);
		}
	}
	
	public List<String> getCurrentPositionStatus() throws FileNotFoundException, IOException {
		List<String> lines = new ArrayList<>();
		String positionFileName = positionFolderName + "/" + variety.name() + "/record.txt";
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(positionFileName)))){
			String line = null;
			String[] pre = null;
			String preLine = null;
			while((line = br.readLine()) != null) {
				String[] parts = line.split(",");
				int direction = Integer.valueOf(parts[1]);
				
				if(pre == null) {
					if(direction != 0) {
						pre = parts;
						preLine = line;
					}
					continue;
				}
				int preD = Integer.valueOf(pre[1]);
				
				if(direction == preD) {
					pre = parts;
					preLine = line;
					continue;
				}
				lines.add(preLine);
				preLine = line;
			}
			lines.add(preLine);
			return lines;
		}
	}

}
