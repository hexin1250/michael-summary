package michael.slf4j.investment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.quant.strategy.FutureStrategy;

@Controller
@RequestMapping(path = "/apps/strategy")
public class StrategyController {
	@Autowired
	private FutureStrategy strategy;
	
	@GetMapping(path = "/mockup")
	public @ResponseBody String schedule(@RequestParam(defaultValue="I") String variety) {
		strategy.mockup(variety, "1MI", 5, 0.4);
		return "ok";
	}

}
