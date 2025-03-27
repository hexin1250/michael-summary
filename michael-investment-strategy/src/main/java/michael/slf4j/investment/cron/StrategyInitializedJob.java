package michael.slf4j.investment.cron;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.constant.TopicConstants;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.proc.PythonExecutor;
import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.research.DataResearchV2;

@Component
@Controller
@EnableScheduling
@PropertySource("classpath:/schedule.properties")
public class StrategyInitializedJob {
	private static final Logger log = Logger.getLogger(StrategyInitializedJob.class);
	
	@Autowired
	private LiveProcessor liveProcessor;
	
	@Autowired
	private DataResearchV2 dataResearchV2;
	
	@Autowired
	MessageService messageService;
	
	@Scheduled(cron = "${clean-schedule}")
	public void cleanData() {
		liveProcessor.afterTrading();
		log.info("[EOD] After trading");
	}

	@Scheduled(cron = "${start-schedule1}")
	public void startNightSchedule() {
		log.info("[Start Night] Before Trading");
		liveProcessor.beforeTrading();
		log.info("[Start Night] Before Trading Done");
	}
	
	@Scheduled(cron = "${start-schedule2}")
	public void startDaySchedule1() {
		log.info("[Start Day 9 o'clock] Before Trading");
		liveProcessor.beforeTrading();
		log.info("[Start Day 9 o'clock] Before Trading Done");
	}
	
	@Scheduled(cron = "${start-schedule3}")
	public void startDaySchedule2() {
		log.info("[Start Day 10:30 o'clock] Before Trading");
		liveProcessor.beforeTrading();
		log.info("[Start Day 10:30 o'clock] Before Trading Done");
	}
	
	@Scheduled(cron = "${start-schedule4}")
	public void startDaySchedule3() {
		log.info("[Start Day 13:30 o'clock] Before Trading");
		liveProcessor.beforeTrading();
		log.info("[Start Day 13:30 o'clock] Before Trading Done");
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void runAfterStartup() {
		log.info("Start to initialize after startup");
		liveProcessor.init();
		log.info("Done to initialize after startup");
	}
	
	@Scheduled(cron = "${summary-night}")
	public void summarizeNightData() {
//		dataResearch.summarize(true, false);
		dataResearchV2.summarize();
		PythonExecutor.executePython();
//		sendMessage();
	}
	@Scheduled(cron = "${summary-afternoon}")
	public void summarizeAfternoonData() {
//		dataResearch.summarize(true, false);
		dataResearchV2.summarize();
		PythonExecutor.executePython();
//		sendMessage();
	}
	@Scheduled(cron = "${summary-close}")
	public void summarizeDayData() {
//		dataResearch.summarize(true, false);
		dataResearchV2.summarize();
		PythonExecutor.executePython();
//		sendMessage();
	}
	
	private void sendMessage() {
		List<String> list = readFile();
		for (String line : list) {
			try {
				messageService.send(TopicConstants.NOTIFICATION_TOPIC, line);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
	private List<String> readFile(){
		String fileName = "C:/Users/HP/python-workspace/myproject/data/reason_output.txt";
		return readFile(fileName);
	}
	
	private static List<String> readFile(String fileName) {
		List<String> ret = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		try {
			List<String> list = getAllLines(fileName);
			for (String line : list) {
				if(sb.length() + line.length() > 1900) {
					ret.add(sb.toString());
					sb = new StringBuffer();
				}
				sb.append(line);
			}
			ret.add(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
			sb.append("Issue happened:").append(e.getMessage());
		}
		return ret;
	}
	
	private static List<String> getAllLines(String fileName) throws IOException{
		return Files.readAllLines(new File(fileName).toPath());
	}

}
