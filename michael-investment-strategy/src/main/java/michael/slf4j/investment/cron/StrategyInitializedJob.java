package michael.slf4j.investment.cron;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.quant.live.LiveProcessor;

@Component
@Controller
@EnableScheduling
@PropertySource("classpath:/schedule.properties")
public class StrategyInitializedJob {
	private static final Logger log = Logger.getLogger(StrategyInitializedJob.class);
	
	@Autowired
	private LiveProcessor liveProcessor;
	
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
}
