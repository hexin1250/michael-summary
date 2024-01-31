package michael.slf4j.investment.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.RealRun;
import michael.slf4j.investment.quant.backtest.ClassicalFutureStrategy;
import michael.slf4j.investment.quant.backtest.MACDStrategy;
import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.quant.mockup.MockupProcess;
import michael.slf4j.investment.quant.strategy.IStrategy;
import michael.slf4j.investment.repo.RealRunRepository;

@Controller
@RequestMapping(path = "/apps/strategy")
public class StrategyController {
	private static final Logger log = Logger.getLogger(StrategyController.class);
	
	private final Map<String, IStrategy> map = new HashMap<>();
	private AtomicInteger atom = new AtomicInteger();
	
	@Autowired
	private MockupProcess process;
	
	@Autowired
	private LiveProcessor liveProcessor;
	
	@Autowired
	private RealRunRepository rrRepo;
	
	@Autowired
	private MessageService messageService;
	
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
		long runId = atom.getAndIncrement();
		log.info("Run ID:" + runId);
		Account acc = new Account(runId, 60000D);
		IStrategy iStrategy = map.get(strategy);
		if(iStrategy == null) {
			switch(strategy) {
			case "macd":
				iStrategy = new MACDStrategy();
				break;
			case "test":
				iStrategy = new ClassicalFutureStrategy();
				break;
				default:
			}
			iStrategy.setMessageService(messageService);
			map.put(strategy, iStrategy);
		}
		process.backtest(runId, acc, iStrategy, start, end);
		return "ok";
	}
	
	/**
	 * http://localhost:1702/apps/strategy/live?strategyName=future&className=michael.slf4j.investment.quant.backtest.ClassicalFutureStrategy&initCash=60000&type=2
	 * @param strategyName
	 * @param className
	 * @param initCash
	 * @param type[1=stock, 2=future]
	 * @return
	 */
	@GetMapping(path = "/live")
	public @ResponseBody String startLive(@RequestParam String strategyName,
			@RequestParam String className, @RequestParam Double initCash,
			@RequestParam int type) {
		RealRun rr = rrRepo.findByName(strategyName);
		if(rr != null) {
			return "Strategy[" + strategyName + "] already exists";
		}
		
		rr = new RealRun();
		rr.setName(strategyName);
		rr.setClassName(className);
		rr.setInitCash(new BigDecimal(initCash));
		rr.setType(type);
		rr.setStartTime(new Timestamp(System.currentTimeMillis()));
		rrRepo.save(rr);
		liveProcessor.initStrategy(rr);
		return "done to initialze:" + strategyName;
	}
	
	/**
	 * http://localhost:1702/apps/strategy/health
	 * @return
	 */
	@GetMapping(path = "/health")
	public @ResponseBody String health() {
		log.info("get request");
		return "ok";
	}
	
	@GetMapping(path = "/status")
	public @ResponseBody String status() {
		StringBuffer sb = new StringBuffer();
		sb.append(new Date()).append("<br>").append(liveProcessor.getStatus());
		log.info("get request to check status[" + sb.toString() + "]");
		return sb.toString();
	}

}
