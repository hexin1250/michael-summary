package michael.slf4j.investment.cron;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	
	private Map<String, String> maps = new ConcurrentHashMap<>();
	private ExecutorService executor = Executors.newFixedThreadPool(30);
	
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
		if(maps.isEmpty()) {
			for (String variety : Constants.VARIETY_LIST) {
				List<String> securitiyList = FutureContract.getFutureContracts(variety);
				securitiyList.stream().forEach(security -> maps.put(security, variety));
			}
		}
		for (Entry<String, String> entry : maps.entrySet()) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						String security = entry.getKey();
						String variety = entry.getValue();
						String content = sinaSource.getContent(entry.getKey());
						futureLoader.load(variety, security, content, FreqEnum._1MI);
					} catch (IOException e) {
						/**
						 * Should not find one security. Ignore this case.
						 */
					}
				}
			});
		}
	}

}
