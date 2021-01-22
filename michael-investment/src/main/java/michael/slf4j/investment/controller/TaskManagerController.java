package michael.slf4j.investment.controller;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.TradeUtil;

@Controller
@RequestMapping(path = "/apps/schedule")
public class TaskManagerController {
	private static final Logger log = Logger.getLogger(TaskManagerController.class);
	@Autowired
	private TaskManager taskManager;
	
	@GetMapping(path = "/task")
	public @ResponseBody String schedule(@RequestParam(defaultValue="I") String varieties) {
		String[] parts = varieties.split(",");
		String ret = Arrays.stream(parts).map(variety -> {
			StringBuffer sb = new StringBuffer();
			boolean result = taskManager.scheduleTask(variety);
			if(result) {
				sb.append("Successful to subscribe [" + variety + "]");
			} else {
				sb.append("Already subscribe [" + variety + "]");
			}
			return sb.toString();
		}).collect(Collectors.joining("<br>"));
		return ret;
	}
	
	@GetMapping(path = "/cancelAllTasks")
	public @ResponseBody String cancelAllTasks() {
		taskManager.cancelTasks();
		log.info("Cancel All Tasks.");
		return "Cancel All Tasks.";
	}
	
	@GetMapping(path = "/showTasks")
	public @ResponseBody String showTasks() {
		String tasks = taskManager.futureMap.keySet().stream().collect(Collectors.joining("<br>"));
		return tasks;
	}
	
	@GetMapping(path = "/isTradingTime")
	public @ResponseBody boolean isTradingTime() {
		boolean isTradingTime = TradeUtil.isTradingTime();
		log.info("Current time is trading time? " + isTradingTime);
		return isTradingTime;
	}
	
	@GetMapping(path = "/isCompleteMunite")
	public @ResponseBody boolean isCompleteMunite() {
		boolean isCompleteMunite = TradeUtil.isCompleteMunite();
		log.info("Current time is complete munite? " + isCompleteMunite);
		return isCompleteMunite;
	}
}
