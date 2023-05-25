package michael.slf4j.investment.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.quant.backtest.MACDStrategy;
import michael.slf4j.investment.quant.backtest.MyTestStrategy;
import michael.slf4j.investment.quant.mockup.MockupProcess;
import michael.slf4j.investment.quant.strategy.IStrategy;

@Controller
@RequestMapping(path = "/apps/strategy")
public class StrategyController {
	private static final Logger log = Logger.getLogger(StrategyController.class);
	
	private final Map<String, IStrategy> map = new HashMap<>();
	
	@Autowired
	private MockupProcess process;
	
	/**
	 * http://localhost:1702/apps/strategy/mockup?strategy=test&variety=I&startDate=2023-04-17&endDate=2023-05-25&dataScope=1&range=1
	 * @param strategy
	 * @param variety
	 * @param startDate
	 * @param endDate
	 * @param dataScope
	 * @param range
	 * @return
	 */
	@GetMapping(path = "/mockup")
	public @ResponseBody String schedule(@RequestParam String strategy, @RequestParam String variety, @RequestParam String startDate,
			@RequestParam String endDate) {
		LocalDate start = LocalDate.parse(startDate);
		LocalDate end = LocalDate.parse(endDate);
		Account acc = new Account(60000D);
		IStrategy iStrategy = map.get(strategy);
		if(iStrategy == null) {
			switch(strategy) {
			case "macd":
				iStrategy = new MACDStrategy();
				break;
			case "test":
				iStrategy = new MyTestStrategy();
				break;
				default:
			}
			map.put(strategy, iStrategy);
		}
		process.backtest(acc, iStrategy, start, end);
		return "ok";
	}
	
	@GetMapping(path = "/health")
	public @ResponseBody String health() {
		log.info("get request");
		return "ok";
	}

}
