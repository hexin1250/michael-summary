package michael.slf4j.investment.research.realtime;

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
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
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
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
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
import michael.slf4j.investment.research.DataResearchV2;
import michael.slf4j.investment.util.IndicatorUtils;
import michael.slf4j.investment.util.PositionFileUtil;
import michael.slf4j.investment.util.StockIndicatorCalculator;
import michael.slf4j.investment.util.TechnicalIndicator;
import michael.slf4j.investment.util.TradeUtil;
import michael.slf4j.investment.util.StockIndicatorCalculator.RSIResult;
import michael.slf4j.investment.util.StockIndicatorCalculator.StockData;
import michael.slf4j.investment.util.TechnicalIndicator.KDJResult;
import michael.slf4j.investment.util.TechnicalIndicator.MACDResult;

@Component
public class RealTimeStrategy implements MessageListener {
	private static final Logger log = Logger.getLogger(RealTimeStrategy.class);
	private static final Pattern FILENAME_PATTERN = Pattern.compile(".*-(.*)-(.*)[.]txt");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	@Autowired
	private TimeseriesRepository timeseriesRepository;
	
	@Autowired
	private DataResearchV2 researchV2;
	
	@Autowired
	@Qualifier("deepSeekProModel")
	private ChatLanguageModel chatModel;

	@Autowired
	MessageService messageService;

	@Value(value = "${future.activemq.topic.15M}")
	private String topic;
	
	@Value(value = "${chat.research.folder}")
	private String researchFolder;
	
	@Value("${chat.flag.folder}")
	private String flagFolderName;
	
	@Value(value = "${chat.user.folder}")
	private String userFolder;
	
	private Map<Variety, String> mainSecurityMap = new HashMap<>();
	private Map<Variety, Integer> countMap = new HashMap<>();
	private Map<Variety, List<ChatMessage>> chatMap = new HashMap<>();
	private Variety variety;
	private NumberFormat nf;
	
	private static final Map<String, String> HEADER_MAP = new LinkedHashMap<>();

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

	public RealTimeStrategy() {
		this.nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setGroupingUsed(false);
		variety = Variety.RB;
	}
	
	@PostConstruct
	public void init() {
		if(!gotoDS) {
			return;
		}
		List<Variety> list = List.of(variety);
		list.parallelStream().forEach(variety -> {
			log.info("Initialize research for [" + variety.name() + "]");
			LocalDateTime current = LocalDateTime.now();
			Timestamp ts = TradeUtil.getTimestamp(current);
			List<String> lastTradeDates = timeseriesRepository.getLast5TradeDate(variety.name(), FreqEnum._1MI.getValue(),
					ts);
			List<String> securityList = timeseriesRepository.getSecurityList(variety.name(), lastTradeDates.get(0));
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
			mainSecurityMap.put(variety, mainSecurity);
			List<ChatMessage> messages = new ArrayList<>();
			chatMap.put(variety, messages);
			String folder = researchFolder + "/" + variety.name() + "/" + mainSecurity;
			File folderDir = new File(folder);
			if(!folderDir.exists()) {
				folderDir.mkdirs();
			}
			String fileNamePrefix = getFilenamePrefix(variety, mainSecurity);
			StringBuffer startSb = new StringBuffer();
			startSb.append("你是一个专业的期货投资顾问,擅长技术分析和解释市场趋势.\n");
			startSb.append("我是激进投资者,我只会100%仓位操作.\n");
			startSb.append("交易说明:我是进行9倍杠杆交易.所以你在生成策略的时候,务必要严谨,我会严格按照你的交易策略进行交易!");
			startSb.append("用中文回答");
			startSb.append("确保输出文本是markdown格式");
			messages.add(SystemMessage.from(startSb.toString()));
			try {
				String[] fileNameArr = folderDir.list();
				List<String> fileNameList = new ArrayList<>();
				Arrays.stream(fileNameArr).forEach(fileName -> fileNameList.add(fileName));
				if(fileNameList.isEmpty()) {
					String initFileName = fileNamePrefix + "0-question.txt";
					researchV2.summarize(variety, true, initFileName);
					messages.add(UserMessage.from(getContent(initFileName)));
					
					Response<AiMessage> initAiReply = chatModel.generate(messages);
					String initReply = initAiReply.content().text();
					messages.add(AiMessage.from(initReply));
					writeFile(fileNamePrefix + "0-answer.txt", initReply);
					
					StringBuffer questionSb = new StringBuffer();
					String myTail = """
稍后,我会提供15分钟和30分钟的数据以及1M的OHLCV和OI数据.请到时候基于更新的数据数据,重新评估日内走势和交易策略(我不会因为隔夜盘而平仓).如果你准备好了,请回复'确认'
我已经要求你用全部的数据进行分析了,为什么你还是只用了部分的数据?请问BAIS/WR/ATR/CCI/MFI/BOLL/MA呢?
历史数据呢,为什么只分析了最新的数据?我给你那么多的历史数据是让你更全面分析的,不是让你直接无视的!!!
请用全部的数据分析!!!
还有,回复我的时候,别告我心态,别说用语言来威胁我.强调策略的时候专业一点!!!
							""";
					questionSb.append(myTail);
					messages.add(UserMessage.from(questionSb.toString()));
					
					Response<AiMessage> contextAiReply = chatModel.generate(messages);
					String contextReply = contextAiReply.content().text();
					messages.add(AiMessage.from(contextReply));
					
					writeFile(fileNamePrefix + "1-question.txt", questionSb.toString());
					writeFile(fileNamePrefix + "1-answer.txt", contextReply);
					
					countMap.put(variety, 2);
					log.info("Done to initialize realtime strategy for " + variety.name());
					
					String flagFileName = flagFolderName + "/" + variety.name() + "-flag.txt";
					LocalDateTime tmp = LocalDateTime.now();
					int currentMinute = tmp.getMinute() - (tmp.getMinute() % 30);
					LocalDateTime latestCheckPoint = LocalDateTime.of(tmp.toLocalDate(), LocalTime.of(tmp.getHour(), currentMinute));
					writeLocalDateTime(latestCheckPoint, flagFileName);
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
			
			List<Timeseries> realTimeList = timeseriesRepository.getAllDataByPeriod(mainSecurity, lastTradeDates.get(0), FreqEnum._15MI.getValue());
			for (int i = 0; i < realTimeList.size(); i++) {
				Timeseries thisTs = realTimeList.get(i);
				researchV2.summarizeDataByFreq(variety, FreqEnum._15MI, thisTs);
				LocalDateTime ldt = TradeUtil.getLocalDateTime(thisTs.getTradeTs());
				if(ldt.getMinute() % 30 == 0 || (ldt.getHour() == 10 && ldt.getMinute() == 15)) {
					Timeseries ts30Min = thisTs.copy();
					if(ldt.getHour() == 10 && ldt.getMinute() == 15) {
						LocalDateTime newLdt = ldt.plusMinutes(15);
						ts30Min.setTradeTs(new Timestamp(TradeUtil.getLong(newLdt)));
					} else {
						Timeseries previousTs = realTimeList.get(i - 1);
						ts30Min.setVolume(ts30Min.getVolume().add(previousTs.getVolume()));
						ts30Min.setOpen(previousTs.getOpen());
						ts30Min.setHigh(new BigDecimal(Math.max(ts30Min.getHigh().doubleValue(), previousTs.getHigh().doubleValue())));
						ts30Min.setLow(new BigDecimal(Math.min(ts30Min.getLow().doubleValue(), previousTs.getLow().doubleValue())));
						ts30Min.setFreq(FreqEnum._30MI.getValue());
					}
					researchV2.summarizeDataByFreq(variety, FreqEnum._30MI, ts30Min);
				}
			}
		});
		log.info("RealTime is ready for all. Main Securities->" + mainSecurityMap
				.entrySet().stream()
				.map(entry -> entry.getKey().name() + ":" + entry.getValue())
				.collect(Collectors.joining(",")));
	}

	private String getFilenamePrefix(Variety variety, String mainSecurity) {
		String fileNamePrefix = researchFolder + "/" + variety.name() + "/" + mainSecurity + "/" + mainSecurity + "-";
		return fileNamePrefix;
	}
	
	public boolean gotoDS = false;
	@SuppressWarnings("unchecked")
	@Override
	@JmsListener(destination = "${future.activemq.topic.15M}")
	public void onMessage(Message message) {
//		if(!gotoDS) {
//			return;
//		}
//		LocalDateTime nowTime = LocalDateTime.now();
//		if((nowTime.getHour() == 21 && nowTime.getMinute() <= 2) || (nowTime.getHour() == 9 && nowTime.getMinute() <= 2) || (nowTime.getHour() == 13 && nowTime.getMinute() <= 32) || nowTime.getHour() == 15) {
//			return;
//		}
//		List<Timeseries> myList = new ArrayList<>();
//		Variety variety = null;
//		try {
//			ObjectMessage objectMessage = (ObjectMessage) message;
//			List<Timeseries> tsList = (List<Timeseries>) objectMessage.getObject();
//			for (Entry<Variety, String> entry : mainSecurityMap.entrySet()) {
//				String mainSecurity = entry.getValue();
//				Timeseries ts = tsList.get(0);
//				if(mainSecurity.equalsIgnoreCase(ts.getSecurity())) {
//					variety = entry.getKey();
//					break;
//				}
//			}
//			if(variety == null) {
//				return;
//			}
//			int nowMin = nowTime.getMinute() - nowTime.getMinute() % 15;
//			log.info("Now Min:" + nowMin);
//			if(nowMin % 30 != 0) {
//				return;
//			}
//			for (Timeseries ts : tsList) {
//				myList.add(ts);
//			}
//			Collections.sort(myList, (a,b) -> (int)(b.getTradeTs().getTime() - a.getTradeTs().getTime()));
//		} catch (Exception e) {
//			log.error("Error during receiving message from the topic:" + topic, e);
//		}
//		startResearch(variety);
	}

	public void startResearch(Variety variety) {
		String flagFileName = flagFolderName + "/" + variety.name() + "-flag.txt";
		log.info("Start to get new research for " + variety.name());
		
		LocalDateTime lastCheckPoint = readLocalDateTime(flagFileName);
		LocalDateTime tmp = LocalDateTime.now();
		int currentMinute = tmp.getMinute() - (tmp.getMinute() % 30);
		LocalDateTime latestCheckPoint = LocalDateTime.of(tmp.toLocalDate(), LocalTime.of(tmp.getHour(), currentMinute));
		if(lastCheckPoint == null) {
			lastCheckPoint = latestCheckPoint;
		}
		
		String mainSecurity = mainSecurityMap.get(variety);
		/**
		 * 15M frequence data
		 */
		
		LocalDateTime current = LocalDateTime.now();
		Timestamp ts = TradeUtil.getTimestamp(current);
		List<String> lastTradeDates = timeseriesRepository.getLast5TradeDate(variety.name(), FreqEnum._1MI.getValue(), ts);
		String tTradeDate = lastTradeDates.get(0);
		
		List<StringBuffer> formatList = new ArrayList<StringBuffer>();
		List<Timeseries> realTime15MList = timeseriesRepository.getAllDataByPeriodFilterEmpty(mainSecurity, tTradeDate, FreqEnum._15MI.getValue());
		Queue<StringBuffer> queue15M = summarizeDataByFreq(FreqEnum._15MI, realTime15MList, lastCheckPoint, latestCheckPoint);
		queue15M.stream().forEach(currentSb -> formatList.add(currentSb));
		Timeseries lastTs = realTime15MList.get(realTime15MList.size() - 1);

		
		StringBuffer sb = new StringBuffer();
		String myHeader = """
根据最新的数据更新分析,着重关注今天这个交易日的走势分析(请做详细说明)
请你分析的时候不要反复打脸,如果反复的方向错误就是你的分析出了问题
请基于全部的历史数据以及最新的提供的数据进行全面分析
				""";
		sb.append(myHeader);
		sb.append("\n");
		sb.append(researchV2.getTableHeader());
		sb.append(formatList.stream().collect(Collectors.joining()));
		sb.append("\n");
		
		Instant lastInstant = lastCheckPoint.atZone(ZoneId.systemDefault()).toInstant();
		Timestamp lastTimestamp = Timestamp.from(lastInstant);
		
		Instant latestInstant = latestCheckPoint.atZone(ZoneId.systemDefault()).toInstant();
		Timestamp latestTimestamp = Timestamp.from(latestInstant);
		
		List<Timeseries> realTime1MList = timeseriesRepository.getAllDataDuringTS(mainSecurity, lastTs.getTradeDate(),
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
		sb.append(_1Msb);
		sb.append("\n");
		sb.append(generateQuestion(variety, lastTs));
		sb.append("\n");
		
		try {
			String userInputFileName = userFolder + "/" + variety.name() + "-input.txt";
			String content = getContent(userInputFileName);
			if(!content.isBlank()) {
				sb.append('\n');
				sb.append('\n');
				StringBuffer userInputSb = new StringBuffer();
				userInputSb.append("用户输入的额外信息:");
				userInputSb.append(content);
				sb.append(userInputSb);
				writeFile(userInputFileName, "");
			}
		} catch (IOException e) {
			log.error("Getting error during user input", e);
		}
		
		String question = sb.toString();
		List<ChatMessage> messages = chatMap.get(variety);
		messages.add(UserMessage.from(question));
		
		log.info("Start real time deepseek for[" + variety + "]");
		Response<AiMessage> aiReply = chatModel.generate(messages);
		String reply = aiReply.content().text();
		messages.add(AiMessage.from(reply));
		
		String fileNamePrefix = getFilenamePrefix(variety, mainSecurity);
		int index = countMap.get(variety);
		try {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("receiver", "Michael小鑫");
			jsonObj.put("variety", variety.name());
			messageService.send(TopicConstants.NOTIFICATION_TOPIC, jsonObj.toString());
			writeFile(fileNamePrefix + index + "-question.txt", question);
			writeFile(fileNamePrefix + index + "-answer.txt", reply);
		} catch (Exception e) {
			e.printStackTrace();
		}
		index++;
		countMap.put(variety, index);
		writeLocalDateTime(latestCheckPoint, flagFileName);
	}
	
	private Queue<StringBuffer> summarizeDataByFreq(FreqEnum freq, List<Timeseries> realTimeTsList, LocalDateTime lastCheckPoint, LocalDateTime latestCheckPoint) {
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
	
	public void cleanup() {
		mainSecurityMap.clear();
		countMap.clear();
		chatMap.clear();
	}
	
	private StringBuffer generateQuestion(Variety variety, Timeseries ts) {
		StringBuffer sb = new StringBuffer();
		LocalDateTime ldt = TradeUtil.getLocalDateTime(ts.getTradeTs());
		String timeStr = ldt.format(DATE_FORMAT);
		BigDecimal closeBD = ts.getClose();
		Map<String, String> map = PositionFileUtil.readPositionData(variety.name());
		StringBuffer anotherCase = new StringBuffer();
		if (!map.isEmpty()) {
			int v = Integer.valueOf(map.get(PositionFileUtil.PRICE));
			int direction = Integer.valueOf(map.get(PositionFileUtil.DIRECTION_INT));
			anotherCase.append(map.get(PositionFileUtil.DIRECTION));
			anotherCase.append("开仓价").append(v).append(",");
			anotherCase.append("浮");
			BigDecimal offset = closeBD.subtract(new BigDecimal(v));
			if(direction * offset.doubleValue() > 0) {
				anotherCase.append("盈");
			} else {
				anotherCase.append("亏");
			}
			String offsetStr = offset.abs().stripTrailingZeros().toPlainString();
			anotherCase.append(offsetStr).append("点,");
			anotherCase.append("仓位").append(map.get(PositionFileUtil.POSITION_PER)).append("%");
		} else {
			anotherCase.append("空仓");
		}
		
		sb.append(timeStr);
		sb.append("\n");
		sb.append(anotherCase);
		return sb;
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
	
	private static void writeLocalDateTime(LocalDateTime dateTime, String fileName) {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))){
        	StringBuffer sb = new StringBuffer();
        	sb.append(dateTime.getYear()).append(" ");
        	sb.append(dateTime.getMonthValue()).append(" ");
        	sb.append(dateTime.getDayOfMonth()).append(" ");
        	sb.append(dateTime.getHour()).append(" ");
        	int minute = dateTime.getMinute() - (dateTime.getMinute() % 30);
        	sb.append(minute);
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

}
