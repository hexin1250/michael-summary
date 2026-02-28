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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import michael.slf4j.investment.util.PositionFileUtil;
import michael.slf4j.investment.util.TradeUtil;

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
	private ChatLanguageModel chatModel;

	@Autowired
	MessageService messageService;

	@Value(value = "${future.activemq.topic.15M}")
	private String topic;
	
	@Value(value = "${chat.research.folder}")
	private String researchFolder;
	
	private Map<Variety, String> mainSecurityMap = new HashMap<>();
	private Map<Variety, Integer> countMap = new HashMap<>();
	private Map<Variety, List<ChatMessage>> chatMap = new HashMap<>();
	
	@PostConstruct
	public void init() {
		List<Variety> list = List.of(Variety.RB, Variety.I);
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
			messages.add(SystemMessage.from("你是一个专业的期货投资顾问,擅长技术分析和解释市场趋势.我是激进投资者,我只会100%仓位操作.用中文回答。"));
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
					questionSb.append("稍后,我会提供15分钟和30分钟的数据.请到时候基于更新的数据数据,重新评估日内走势和交易策略(我不会因为隔夜盘而平仓).如果你准备好了,请回复'确认'\n");
					questionSb.append("注意:需要省略不必要的markdown格式带来的字符*等");
					messages.add(UserMessage.from(questionSb.toString()));
					
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
	
	@SuppressWarnings("unchecked")
	@Override
	@JmsListener(destination = "${future.activemq.topic.15M}")
	public void onMessage(Message message) {
		List<Timeseries> myList = new ArrayList<>();
		Variety variety = null;
		try {
			ObjectMessage objectMessage = (ObjectMessage) message;
			List<Timeseries> tsList = (List<Timeseries>) objectMessage.getObject();
			for (Entry<Variety, String> entry : mainSecurityMap.entrySet()) {
				String mainSecurity = entry.getValue();
				Timeseries ts = tsList.get(0);
				if(mainSecurity.equalsIgnoreCase(ts.getSecurity())) {
					variety = entry.getKey();
					break;
				}
			}
			if(variety == null) {
				return;
			}
			for (Timeseries ts : tsList) {
				myList.add(ts);
			}
			Collections.sort(myList, (a,b) -> (int)(b.getTradeTs().getTime() - a.getTradeTs().getTime()));
		} catch (Exception e) {
			log.error("Error during receiving message from the topic:" + topic, e);
		}
		String mainSecurity = mainSecurityMap.get(variety);
		Timeseries lastTs1 = myList.get(0);
		Timeseries lastTs2 = myList.get(1);
		Timeseries lastTs3 = myList.get(2);
		Timeseries lastTs = lastTs1;
		Timeseries nextTs = lastTs2;
		if(lastTs1.getTradeTs().getTime() > System.currentTimeMillis()) {
			lastTs = lastTs2;
			nextTs = lastTs3;
		}
		
		StringBuffer sb = new StringBuffer();
		String myHeader = """
本次分析基于【日线/周线】级别持仓.我的风格是100%仓位激进操作,但风控必须匹配我的持仓周期.请遵循以下分析流程:
1.首先确认我的持仓周期和逻辑
2.确认最新提供的数据并实时可能的走势
3.明确在什么价位将100%仓位做反手操作,而不是基于分钟图频繁止损
4.宽幅止损:应基于我持仓级别的关键支撑/压力，而非日内波动
5.最后,结合分时指标等作为辅助验证和出场点细化.回答应保持简洁的同时,尽可能提供更多信息
注:严格确保输出在1000个token以内
				""";
		sb.append(myHeader);
		sb.append("\n");
		sb.append(researchV2.getHeader());
		sb.append(researchV2.summarizeDataByFreq(variety, FreqEnum._15MI, lastTs));
		LocalDateTime ldt = TradeUtil.getLocalDateTime(lastTs.getTradeTs());
		if(ldt.getMinute() % 30 == 0 || (ldt.getHour() == 10 && ldt.getMinute() == 15)) {
			Timeseries ts30Min = lastTs.copy();
			if(ldt.getHour() == 10 && ldt.getMinute() == 15) {
				LocalDateTime newLdt = ldt.plusMinutes(15);
				ts30Min.setTradeTs(new Timestamp(TradeUtil.getLong(newLdt)));
			} else {
				ts30Min.setVolume(ts30Min.getVolume().add(nextTs.getVolume()));
				ts30Min.setOpen(nextTs.getOpen());
				ts30Min.setHigh(new BigDecimal(Math.max(ts30Min.getHigh().doubleValue(), nextTs.getHigh().doubleValue())));
				ts30Min.setLow(new BigDecimal(Math.min(ts30Min.getLow().doubleValue(), nextTs.getLow().doubleValue())));
				ts30Min.setFreq(FreqEnum._30MI.getValue());
			}
			sb.append(researchV2.summarizeDataByFreq(variety, FreqEnum._30MI, ts30Min));
		}
		
		sb.append(generateQuestion(variety, lastTs));
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
			messageService.send(TopicConstants.NOTIFICATION_TOPIC, reply);
			writeFile(fileNamePrefix + index + "-question.txt", question);
			writeFile(fileNamePrefix + index + "-answer.txt", reply);
		} catch (Exception e) {
			e.printStackTrace();
		}
		index++;
		countMap.put(variety, index);
		
	}
	
	public void cleanup() {
		mainSecurityMap.clear();
		countMap.clear();
		chatMap.clear();
	}
	
	private String generateQuestion(Variety variety, Timeseries ts) {
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
		return sb.toString();
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

}
