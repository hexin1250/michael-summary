package michael.slf4j.investment.cron;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.etl.DataLoaderClient;
import michael.slf4j.investment.etl.DataResearch;
import michael.slf4j.investment.taskmanager.TaskManager;

@Component
@Controller
@PropertySource("classpath:/schedule.properties")
//@PropertySource("file:src/main/resources/schedule.properties")
public class ScheduleJob {
	private static final Logger log = Logger.getLogger(ScheduleJob.class);
	
	@Autowired
	private TaskManager taskManager;
	
	@Autowired
	@Qualifier(value="dataLoaderClient")
	private DataLoaderClient dataLoaderClient;
	
	@Autowired
	private DataResearch dataResearch;
	
	@Scheduled(cron = "${clean-schedule}")
	public void cleanData() {
		taskManager.fillBack1D();
		log.info("[EOD] Done to fill back 1D data.");
	}

	@Scheduled(cron = "${start-schedule1}")
	public void startNightSchedule() {
		log.info("[Start Night] subscribe all varieties.");
		taskManager.subscribeSecurities();
		log.info("[Start Night] Done");
	}
	
	@Scheduled(cron = "${end-schedule1}")
	public void endNightSchedule() {
		log.info("[End Night] Done.");
	}
	
	@Scheduled(cron = "${start-schedule2}")
	public void startDaySchedule1() {
		log.info("[Start Day 9 o'clock] subscribe all varieties.");
		taskManager.subscribeSecurities();
		log.info("[Start Day 9 o'clock] Done");
	}
	
	@Scheduled(cron = "${end-schedule2}")
	public void endDaySchedule1() {
		log.info("[End 10:15] Done.");
	}
	
	@Scheduled(cron = "${start-schedule3}")
	public void startDaySchedule2() {
		log.info("[Start Day 10:30 o'clock] subscribe all varieties.");
		taskManager.subscribeSecurities();
		log.info("[Start Day 10:30 o'clock] Done");
	}
	
	@Scheduled(cron = "${end-schedule3}")
	public void endDaySchedule2() {
		log.info("[End 11:30] Done.");
	}
	
	@Scheduled(cron = "${start-schedule4}")
	public void startDaySchedule3() {
		log.info("[Start Day 13:30 o'clock] subscribe all varieties.");
		taskManager.subscribeSecurities();
		log.info("[Start Day 13:30 o'clock] Done");
	}
	
	@Scheduled(cron = "${end-schedule4}")
	public void endDaySchedule3() {
		log.info("[End 15:00] Done.");
	}
	
	@Scheduled(cron = "${update-minute}")
	public void updateMinuteData() {
		dataLoaderClient.update1MinData();
	}
	
	@Scheduled(cron = "${update-15-night}")
	public void updateNightData() {
		dataLoaderClient.update15MinData();
	}
	@Scheduled(cron = "${update-15-afternoon}")
	public void updateAfternoonData() {
		dataLoaderClient.update15MinData();
	}
	
	@Scheduled(cron = "${summary-night}")
	public void summarizeNightData() {
		dataResearch.summarize();
	}
	@Scheduled(cron = "${summary-afternoon}")
	public void summarizeAfternoonData() {
		dataResearch.summarize();
	}
	@Scheduled(cron = "${summary-close}")
	public void summarizeDayData() {
		dataResearch.summarize();
	}

}
