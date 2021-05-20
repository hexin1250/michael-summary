package michael.slf4j.investment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.quant.BacktestRequest;
import michael.slf4j.investment.quant.backtest.FutureStrategy;

@Controller
@RequestMapping(path = "/apps/strategy")
public class StrategyController {
	@Autowired
	private FutureStrategy strategy;
	
	@GetMapping(path = "/mockup")
	public @ResponseBody String schedule(@RequestParam String variety, @RequestParam String freq, @RequestParam String startDate, @RequestParam String endDate, @RequestParam int dataScope, @RequestParam double range) {
		BacktestRequest request = new BacktestRequest(variety, freq, startDate, endDate);
		strategy.mockup(request, dataScope, range);
		return "ok";
	}

}
