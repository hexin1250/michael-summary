package michael.slf4j.investment.controller;

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
	public @ResponseBody String schedule(@RequestParam(defaultValue="I") String variety) {
		boolean result = taskManager.scheduleTask(variety);
		if(result) {
			return "successful";
		} else {
			return "already scheduled";
		}
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
