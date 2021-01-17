package michael.slf4j.investment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.taskmanager.TaskManager;

@Controller
@RequestMapping(path = "/apps/schedule")
public class TaskManagerController {
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
}
