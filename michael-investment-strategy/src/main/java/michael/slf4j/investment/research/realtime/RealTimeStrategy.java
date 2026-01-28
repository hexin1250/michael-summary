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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.stereotype.Service;

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

@Service
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

	@Value(value = "${future.activemq.topic}")
	private String topic;
	
	@Value(value = "${chat.research.folder}")
	private String researchFolder;
	
	private Map<Variety, String> mainSecurityMap = new HashMap<>();
	private Map<Variety, Integer> countMap = new HashMap<>();
	private Map<Variety, List<ChatMessage>> chatMap = new HashMap<>();
	private Map<Variety, Timeseries> tsMap = new HashMap<>();
	
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
					questionSb.append("现在我需要实时盯盘,基于每分钟的数据.开盘之后,我会给你当前分钟时间,当前最新价,当前OI和当前持仓情况.\n");
					questionSb.append("到时候我需要你根据实时数据,重新评估日内走势,然后生成最新的策略(我不会因为隔夜盘而平仓)\n");
					questionSb.append("注意:输出要控制在100个字符内,需要省略不必要的markdown格式带来的字符*等");
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
					countMap.put(variety, index);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			log.info("RealTime is ready for " + variety.name());
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
	@JmsListener(destination = "${future.activemq.topic}")
	public void onMessage(Message message) {
		Map<String, Timeseries> map = new HashMap<>();
		List<Timeseries> tsList = null;
		try {
			ObjectMessage objectMessage = (ObjectMessage) message;
			tsList = (List<Timeseries>) objectMessage.getObject();
			for (Timeseries ts : tsList) {
				map.put(ts.getSecurity(), ts);
			}
		} catch (Exception e) {
			log.error("Error during receiving message from the topic:" + topic, e);
		}
		List<Variety> list = List.of(Variety.RB, Variety.I);
		list.parallelStream().forEach(variety -> {
			String mainSecurity = mainSecurityMap.get(variety);
			Timeseries ts = map.get(mainSecurity);
			Timeseries lastTs = tsMap.get(variety);
			boolean moreInfo = false;
			if(lastTs != null) {
				BigDecimal prevBuy1 = lastTs.getBuy1();
				BigDecimal prevSell1 = lastTs.getSell1();
				BigDecimal buy1 = ts.getBuy1();
				BigDecimal sell1 = ts.getSell1();
				if(prevBuy1 != null && prevBuy1.compareTo(buy1) == 0 && prevSell1 != null && prevSell1.compareTo(sell1) == 0) {
					return;
				}
				BigDecimal offset = null;
				if(prevBuy1 != null && buy1 != null) {
					offset = prevBuy1.subtract(buy1).abs();
				} else {
					offset = prevSell1.subtract(sell1).abs();
				}
				switch(variety) {
				case RB:
					if(offset.intValue() > 3) {
						moreInfo = true;
					}
					break;
				case I:
					if(offset.doubleValue() > 1.5D) {
						moreInfo = true;
					}
					break;
					default:
						moreInfo = false;
				}
			}
			LocalTime lt = LocalTime.now();
			if(lt.getMinute() % 30 == 0) {
				moreInfo = true;
			}
			StringBuffer sb = new StringBuffer();
			sb.append(generateQuestion(variety, ts));
			sb.append("\n");
			if(!moreInfo) {
				sb.append("输出严格控制在50字符以内");
			} else {
				sb.append("需要详细信息,输出在200字符以内");
			}
			String question = sb.toString();
			List<ChatMessage> messages = chatMap.get(variety);
			messages.add(UserMessage.from(question));
			
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
			tsMap.put(variety, ts);
		});
		
	}
	
	public void cleanup() {
		mainSecurityMap.clear();
		countMap.clear();
		chatMap.clear();
		tsMap.clear();
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
		sb.append(" ");
		sb.append(closeBD.stripTrailingZeros().toPlainString());
		sb.append(" ");
		sb.append(ts.getOpenInterest().stripTrailingZeros().toPlainString());
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
