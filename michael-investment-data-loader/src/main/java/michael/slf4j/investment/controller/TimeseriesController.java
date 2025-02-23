package michael.slf4j.investment.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.repo.TimeseriesRepository;

@Controller
@RequestMapping(path = "/apps/ts")
public class TimeseriesController {
	@Autowired
	private TimeseriesRepository timeseriesRepository;
	
	@GetMapping(path = "/list")
	public @ResponseBody String getTs(@RequestParam(defaultValue="") String security) {
		List<Timeseries> list = timeseriesRepository.findBySecurity(security);
		return "size:" + list.size();
	}
	
	@GetMapping(path = "/listTradeDate")
	public @ResponseBody List<String> getAllTradeDate() {
		List<String> list = timeseriesRepository.findAllTradeDate();
		return list;
	}
}
