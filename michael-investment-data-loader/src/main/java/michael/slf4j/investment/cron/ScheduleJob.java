package michael.slf4j.investment.cron;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.constant.Constants;
import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.source.SinaSource;
import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.FutureContract;
import michael.slf4j.investment.util.TradeUtil;

@Component
@Controller
@PropertySource("classpath:/schedule.properties")
public class ScheduleJob {
	
	@Autowired
	private TaskManager taskManager;
	
	@Autowired
	private FutureLoader futureLoader;
	
	@Autowired
	private SinaSource sinaSource;
	
	@Scheduled(cron = "${clean-schedule}")
	public void cleanData() {
		taskManager.cancelTasks();
		taskManager.fillBack1D();
	}

	@Scheduled(cron = "${update-primary}")
	public void initPrimaryContract() {
		taskManager.init();
		taskManager.subscribeAll();
	}
	
	@Scheduled(cron = "${update-minute}")
	public void updateMinuteData() {
		if(!TradeUtil.isTradingTime()) {
			return;
		}
		Constants.VARIETY_LIST.parallelStream().forEach(variety -> {
			List<String> securitiyList = FutureContract.getFutureContracts(variety);
			securitiyList.parallelStream().forEach(security -> {
				try {
					String content = sinaSource.getContent(security);
					futureLoader.load(variety, security, content, FreqEnum._1MI);
				} catch (IOException e) {
					/**
					 * Should not find one security. Ignore this case.
					 */
				}
			});
		});
	}

}
