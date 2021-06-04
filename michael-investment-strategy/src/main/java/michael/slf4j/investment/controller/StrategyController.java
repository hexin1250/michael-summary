package michael.slf4j.investment.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.quant.backtest.MACDStrategy;
import michael.slf4j.investment.quant.backtest.MyTestStrategy;
import michael.slf4j.investment.quant.mockup.RunningProcess;
import michael.slf4j.investment.quant.strategy.IStrategy;

@Controller
@RequestMapping(path = "/apps/strategy")
public class StrategyController {
	private final Map<String, IStrategy> map = new HashMap<>();
	
	@Autowired
	private RunningProcess process;
	
	@GetMapping(path = "/mockup")
	public @ResponseBody String schedule(@RequestParam String strategy, @RequestParam String variety, @RequestParam String startDate, @RequestParam String endDate, @RequestParam int dataScope, @RequestParam double range) {
		LocalDate start = LocalDate.parse(startDate);
		LocalDate end = LocalDate.parse(endDate);
		Account acc = new Account(60000D);
		IStrategy istrategy = map.get(strategy);
		if(istrategy == null) {
			switch(strategy) {
			case "macd":
				istrategy = new MACDStrategy();
				break;
			case "test":
				istrategy = new MyTestStrategy();
				break;
				default:
			}
			map.put(strategy, istrategy);
		}
		process.backtest(acc, istrategy, start, end);
		return "ok";
	}

}
