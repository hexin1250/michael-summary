package michael.slf4j.investment.cron;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.source.ISource;
import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.TradeUtil;

@Component
@Controller
@PropertySource("classpath:/schedule.properties")
public class ScheduleJob {
	private static final Logger log = Logger.getLogger(ScheduleJob.class);
	
	@Autowired
	private TaskManager taskManager;
	
	@Autowired
	private FutureLoader futureLoader;
	
	@Autowired
	@Qualifier(value="aliSource")
	private ISource source;
	
	@Autowired
	@Qualifier(value="aliParser")
	private IParser parser;
	
	@Autowired
	@Qualifier(value="currentSecurities")
	private Set<String> futureSecurities;
	
	private ExecutorService executor = Executors.newFixedThreadPool(30);
	
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
		log.info("[End Night] unregister all varieties.");
		taskManager.cancelTasks();
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
		log.info("[End 10:15] unregister all varieties.");
		taskManager.cancelTasks();
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
		log.info("[End 11:30] unregister all varieties.");
		taskManager.cancelTasks();
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
		log.info("[End 15:00] unregister all varieties.");
		taskManager.cancelTasks();
		log.info("[End 15:00] Done.");
	}
	
	@Scheduled(cron = "${update-minute}")
	public void updateMinuteData() {
		if(!TradeUtil.isTradingTime()) {
			return;
		}
		taskManager.subscribeSecurities();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					String content = source.getContent(futureSecurities);
					futureLoader.loadMultiSecurities(parser, content, FreqEnum._1MI);
				} catch (IOException e) {
					/**
					 * Should not find one security. Ignore this case.
					 */
				}
			}
		});
	}

}
