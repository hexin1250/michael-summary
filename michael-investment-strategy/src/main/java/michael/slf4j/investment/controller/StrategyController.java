package michael.slf4j.investment.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.quant.backtest.MyTestStrategy;
import michael.slf4j.investment.quant.mockup.RunningProcess;

@Controller
@RequestMapping(path = "/apps/strategy")
public class StrategyController {
	@Autowired
	private RunningProcess process;
	
	@GetMapping(path = "/mockup")
	public @ResponseBody String schedule(@RequestParam String variety, @RequestParam String freq, @RequestParam String startDate, @RequestParam String endDate, @RequestParam int dataScope, @RequestParam double range) {
		LocalDate start = LocalDate.parse(startDate);
		LocalDate end = LocalDate.parse(endDate);
		Account acc = new Account(20000D);
		process.backtest(acc, new MyTestStrategy(), start, end);
		return "ok";
	}

}
