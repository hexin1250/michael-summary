package michael.slf4j.investment.cron;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.research.XAUDataResearch;
import michael.slf4j.investment.research.realtime.RealTimeStrategy;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.util.ResearchUtil;

@Component
@Controller
@EnableScheduling
@PropertySource("classpath:/schedule.properties")
public class StrategyInitializedJob {
	private static final Logger log = Logger.getLogger(StrategyInitializedJob.class);
	
	@Autowired
	private LiveProcessor liveProcessor;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private ResearchUtil researchUtil;
	
	@Autowired
	private RealTimeStrategy realtimeStrategy;
	
	@Autowired
	private XAUDataResearch xauDataResearch;
	
	@Value("${chat.history.folder}")
	private String historyFolderName;
	
	@Value("${chat.backup.folder}")
	private String backupFolderName;
	
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
		
		log.info("Doing housekeeping");
		fileService.housekeeping();
		realtimeStrategy.cleanup();
		log.info("Done housekeeping");
		
		log.info("Start to initialize real time strategy");
		realtimeStrategy.init();
		log.info("Done to initialize real time strategy");
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
	
	@Scheduled(cron = "${top-deal-close}")
	public void summarizeData4RB() throws FileNotFoundException, IOException {
		researchUtil.doResearch(Variety.RB);
	}
	
	@Scheduled(cron = "${top-deal-close2}")
	public void summarizeData4I() throws FileNotFoundException, IOException {
		researchUtil.doResearch(Variety.I);
	}
	
	@Scheduled(cron = "${xauusd-research}")
	public void summarizeXAUUSD() throws FileNotFoundException, IOException {
		xauDataResearch.summarize();
	}

}
