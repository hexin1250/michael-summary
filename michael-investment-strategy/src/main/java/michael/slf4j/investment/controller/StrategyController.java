package michael.slf4j.investment.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.RealRun;
import michael.slf4j.investment.proc.PythonExecutor;
import michael.slf4j.investment.quant.backtest.ClassicalFutureStrategy;
import michael.slf4j.investment.quant.backtest.MACDStrategy;
import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.quant.mockup.MockupProcess;
import michael.slf4j.investment.quant.strategy.IStrategy;
import michael.slf4j.investment.repo.RealRunRepository;
import michael.slf4j.investment.research.DataResearchV2;
import michael.slf4j.investment.util.MarkdownUtil;
import michael.slf4j.investment.util.PositionFileUtil;

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

	@Autowired
	private DataResearchV2 dataResearchV2;

	/**
	 * http://localhost:1702/apps/strategy/mockup?strategy=test&variety=I&startDate=2023-04-17&endDate=2023-05-25
	 * 
	 * @param strategy
	 * @param variety
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@GetMapping(path = "/mockup")
	public @ResponseBody String schedule(@RequestParam String strategy, @RequestParam String variety,
			@RequestParam String startDate, @RequestParam String endDate) {
		LocalDate start = LocalDate.parse(startDate);
		LocalDate end = LocalDate.parse(endDate);
		long runId = atom.getAndIncrement();
		log.info("Run ID:" + runId);
		Account acc = new Account(runId, 33000D);
		IStrategy iStrategy = map.get(strategy);
		if (iStrategy == null) {
			switch (strategy) {
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
	 * 
	 * @param strategyName
	 * @param className
	 * @param initCash
	 * @param type[1=stock, 2=future]
	 * @return
	 */
	@GetMapping(path = "/live")
	public @ResponseBody String startLive(@RequestParam String strategyName, @RequestParam String className,
			@RequestParam Double initCash, @RequestParam int type) {
		RealRun rr = rrRepo.findByName(strategyName);
		if (rr != null) {
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
	 * 
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

	/**
	 * http://localhost:1702/apps/strategy/research
	 * http://localhost:1702/apps/strategy/research?security=RB2510
	 * http://localhost:1702/apps/strategy/research?latest=true
	 * 
	 * @param full
	 * @return
	 */
	@GetMapping(path = "/research")
	public @ResponseBody String research(
			@RequestParam(name = "security", required = false, defaultValue = "") String security) {
		dataResearchV2.summarize(security);
		StringBuffer sb = new StringBuffer();
		sb.append(new Date()).append("<br>").append("research is done.");
		log.info("Do research");
		return sb.toString();
	}

	/**
	 * http://localhost:1702/apps/strategy/savePosition?direction=-1&price=3330&position=25
	 * 
	 * @param direction
	 * @param price
	 * @param positionPer
	 * @return
	 */
	@GetMapping(path = "/savePosition")
	public @ResponseBody String savePosition(
			@RequestParam(name = "direction", required = false, defaultValue = "0") int direction,
			@RequestParam(name = "price", required = false, defaultValue = "0") int price,
			@RequestParam(name = "position", required = false, defaultValue = "0") int positionPer) {
		PositionFileUtil.savePositionData(direction, price, positionPer);
		StringBuffer sb = new StringBuffer();
		sb.append(new Date()).append("<br>").append("Done to save data.");
		log.info("Save position data");
		return sb.toString();
	}

	/**
	 * http://localhost:1702/apps/strategy/deepseek
	 * 
	 * @return
	 */
	@GetMapping(path = "/deepseek")
	public @ResponseBody String deepseek() {
		log.info("get request to deepseek");
		int ret = PythonExecutor.executePython();
		StringBuffer sb = new StringBuffer();
		if (ret != 0) {
			sb.append(new Date()).append("<br>").append("Done to deepseek, but request failed, please check.");
		}
		sb.append(PositionFileUtil.getDeepseek());
		return sb.toString();
	}

	/**
	 * http://localhost:1702/apps/strategy/deepseekHistory
	 * 
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@GetMapping(path = "/deepseekHistory")
	public String deepseekHistory(Model model) throws Exception {
		log.info("Check new deepseek history page");
		File file = new File("C:/Users/HP/python-workspace/myproject/data/reason_output.txt");
		try (InputStream is = new FileInputStream(file)) {
			long timestamp = file.lastModified(); // 获取时间戳（毫秒）
			Date date = new Date(timestamp);
			StringBuffer sb = new StringBuffer();
			sb.append("## ");
			sb.append(date);
			sb.append("\n");
			byte[] a = sb.toString().getBytes();
			byte[] b = FileCopyUtils.copyToByteArray(is);
			byte[] bytes = new byte[a.length + b.length];
			System.arraycopy(a, 0, bytes, 0, a.length);
			System.arraycopy(b, 0, bytes, a.length, b.length);

			String markdown = new String(bytes, "UTF-8");

			// 转换为HTML
			String htmlContent = MarkdownUtil.convertToHtml(markdown);
			model.addAttribute("content", htmlContent);

			return "markdown-page";
		}
	}

	@GetMapping(path = "/deepseekHistoryV2")
	public @ResponseBody String deepseekHistoryV2() {
		log.info("Check deepseek history");
		StringBuffer sb = new StringBuffer();
		sb.append(PositionFileUtil.getDeepseek());
		return sb.toString();
	}

	/**
	 * http://localhost:1702/apps/strategy/question
	 * 
	 * @return
	 */
	@GetMapping(path = "/question")
	public @ResponseBody String question() {
		log.info("Get question");
		StringBuffer sb = new StringBuffer();
		sb.append(PositionFileUtil.getQuestion());
		return sb.toString();
	}

}
