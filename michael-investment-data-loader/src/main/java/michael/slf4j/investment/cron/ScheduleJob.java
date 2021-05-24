package michael.slf4j.investment.cron;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.taskmanager.TaskManager;

@Component
@Controller
@PropertySource("classpath:/schedule.properties")
public class ScheduleJob {
	@Autowired
	private TaskManager taskManager;
	
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

}
