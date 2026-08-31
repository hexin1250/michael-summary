package michael.slf4j.investment.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.backtest.ClassicalFutureStrategy;
import michael.slf4j.investment.quant.backtest.MACDStrategy;
import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.quant.mockup.MockupProcess;
import michael.slf4j.investment.quant.strategy.IStrategy;
import michael.slf4j.investment.repo.RealRunRepository;
import michael.slf4j.investment.research.DataResearchV2;
import michael.slf4j.investment.research.MyChatBot;
import michael.slf4j.investment.research.XAUDataResearch;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.service.StatelessChatService;
import michael.slf4j.investment.util.MarkdownUtil;
import michael.slf4j.investment.util.PositionFileUtil;

@Controller
@RequestMapping(path = "/apps/strategy")
public class StrategyController {
	private static final Logger log = Logger.getLogger(StrategyController.class);

	private final Map<String, IStrategy> map = new HashMap<>();
	private AtomicInteger atom = new AtomicInteger();
	
	@Value("${chat.history.folder}")
	private String historyFolderName;
	
	@Value("${chat.backup.folder}")
	private String backupFolderName;
	
	@Value("${chat.point.folder}")
	private String pointFolderName;

	@Value("${chat.position.folder}")
	private String positionFolderName;

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
	
	@Autowired
	private StatelessChatService statelessChatService;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private XAUDataResearch xauDataResearch;
	
	@Value(value = "${chat.research.folder}")
	private String researchFolder;

	@Value(value = "${chat.user.folder}")
	private String userFolder;
	
	@Autowired
	private MyChatBot chat;
	
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
	 * http://localhost:1702/apps/strategy/research?variety=I
	 * 
	 * @param full
	 * @return
	 */
	@GetMapping(path = "/research")
	public @ResponseBody String research(
			@RequestParam(name = "variety", required = true) String varietyStr) {
		File historyFolder = new File(historyFolderName + "/" + varietyStr);
		File[] files = historyFolder.listFiles();
		Variety variety = Variety.of(varietyStr);
		String tradeDate = fileService.getTradeDate(variety);
		long count = Arrays.stream(files).filter(file -> !file.getName().contains(tradeDate)).count();
		dataResearchV2.summarize(variety, count == 0);
		StringBuffer sb = new StringBuffer();
		sb.append(new Date()).append("<br>").append("research is done.");
		log.info("Do research");
		return sb.toString();
	}

	/**
	 * http://localhost:1702/apps/strategy/savePosition?direction=-1&price=3330&position=25&variety=RB
	 * 
	 * @param direction
	 * @param price
	 * @param positionPer
	 * @return
	 */
	@GetMapping(path = "/savePosition")
	public @ResponseBody String savePosition(
			@RequestParam(name = "variety", required = true) String variety,
			@RequestParam(name = "direction", required = false, defaultValue = "0") int direction,
			@RequestParam(name = "price", required = false, defaultValue = "0") String price,
			@RequestParam(name = "position", required = false, defaultValue = "0") int positionPer) {
		PositionFileUtil.savePositionData(variety, direction, price, positionPer);
		StringBuffer sb = new StringBuffer();
		sb.append(new Date()).append("<br>").append("Done to save data.");
		
		String positionFileName = positionFolderName + "/" + variety + "/record.txt";
		StringBuffer appendSb = new StringBuffer();
		appendSb.append(variety).append(",");
		appendSb.append(direction).append(",");
		appendSb.append(price).append(",");
		appendSb.append(positionPer).append(",");
		appendSb.append(LocalDateTime.now());
        String content = appendSb.toString();
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(positionFileName, true))) {
            bw.write(content);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		log.info("Save position data");
		return sb.toString();
	}

	/**
	 * http://localhost:1702/apps/strategy/deepseek?variety=RB
	 * 
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@GetMapping(path = "/deepseek")
	public @ResponseBody String deepseek(@RequestParam(name = "variety", required = true) String varietyStr) throws FileNotFoundException, IOException {
		log.info("get request to deepseek");
		Variety variety = Variety.of(varietyStr);
		statelessChatService.doResearch(variety);
		log.info("Done to get response from deepseek");
		return "ok";
	}

	/**
	 * http://localhost:1702/apps/strategy/deepseekHistory?variety=RB
	 * 
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@GetMapping(path = "/deepseekHistory")
	public String deepseekHistory(Model model, @RequestParam(name = "variety", required = true) String variety) throws Exception {
		log.info("Check new deepseek history page");
		File historyFolder = new File(historyFolderName + "/" + variety);
		File[] files = historyFolder.listFiles();
		if(files.length == 0) {
			File backupFolder = new File(backupFolderName + "/" + variety);
			files = backupFolder.listFiles();
		}
		File file = Arrays.stream(files).filter(a -> a.getName().contains("answer.txt")).max((a, b) -> {
			return a.getName().compareTo(b.getName());
		}).get();
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
	
	/**
	 * http://localhost:1702/apps/strategy/realtime?variety=RB
	 * 
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@GetMapping(path = "/realtime")
	public String deepseekXAUHistory(Model model, @RequestParam(name = "variety", required = true) String variety) throws Exception {
		log.info("Check new deepseek history page");
		File historyFolder = new File(researchFolder + "/" + variety);
		File[] fileArray = historyFolder.listFiles();
		if(fileArray.length == 0) {
			return "empty";
		}
		File[] files = null;
		if(fileArray[0].isDirectory()) {
			File dir = Arrays.stream(fileArray).max((a, b) -> a.compareTo(b)).get();
			files = dir.listFiles();
		} else {
			files = fileArray;
		}
		File file = Arrays.stream(files).filter(a -> a.getName().contains("answer.txt")).filter(a -> !a.getName().contains("-1-")).max((a, b) -> {
			String[] partsA = a.getName().split("[-]");
			String[] partsB = b.getName().split("[-]");
			int numberA = Integer.valueOf(partsA[1]);
			int numberB = Integer.valueOf(partsB[1]);
			return numberA - numberB;
		}).get();
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
	
	/**
	 * http://localhost:1702/apps/strategy/question?variety=I
	 * 
	 * @return
	 */
	@GetMapping(path = "/question")
	public @ResponseBody String question(@RequestParam(name = "variety", required = true) String variety) {
		log.info("Get question");
		StringBuffer sb = new StringBuffer();
		sb.append(PositionFileUtil.readFile(fileService.getQuestionFileName(variety)));
		return sb.toString();
	}
	
	/**
	 * http://localhost:1702/apps/strategy/cleanupXAU
	 * 
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@GetMapping(path = "/cleanupXAU")
	public @ResponseBody String cleanupXAUUSD() throws FileNotFoundException, IOException {
		log.info("Cleanup XAUUSD");
		xauDataResearch.cleanup();
		log.info("Done to cleanup XAUUSD");
		return "cleanup XAUUSD";
	}
	
	/**
	 * http://localhost:1702/apps/strategy/convert
	 * 
	 * @return
	 * @throws IOException 
	 */
	@GetMapping(path = "/convert")
	public String convert(Model model) throws IOException {
		log.info("Converting...");
		File file = new File("C:/Users/HP/python-workspace/myproject/data/convert.txt");
		try (InputStream is = new FileInputStream(file)) {
			StringBuffer sb = new StringBuffer();
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
	
	/**
	 * http://localhost:1702/apps/strategy/input?variety=XAUUSD
	 * 
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	@GetMapping(path = "/input")
	public @ResponseBody String userInput(@RequestParam(name = "variety", required = true) String variety,
			@RequestParam(name = "input", required = true) String input) throws FileNotFoundException, IOException {
		log.info("User input:" + input);
		String userInputFileName = userFolder + "/" + variety + "-input.txt";
		writeFile(userInputFileName, input);
		return "complete to write[" + input + "]";
	}
	
	private void writeFile(String fileName, String content) throws FileNotFoundException, IOException {
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))){
			bw.write(content);
			bw.newLine();
			bw.flush();
		}
	}
	
	/**
	 * http://localhost:1702/apps/strategy/position?variety=XAUUSD
	 */
	@GetMapping(path = "/position")
	public @ResponseBody String getPosition(@RequestParam(name = "variety", required = true) String variety) throws FileNotFoundException, IOException {
		BigDecimal bd = xauDataResearch.getCurrentPosition();
		BigDecimal ret = bd.setScale(4, RoundingMode.HALF_UP);
		log.info("Current Position Status:[" + ret + "]");
		StringBuffer sb = new StringBuffer();
		sb.append(xauDataResearch.getCurrentPositionStatus().stream().map(v -> "<br>" + v).collect(Collectors.joining()));
		return "Current Position Status:[" + ret + "]" + sb.toString();
	}
	
	@GetMapping(path = "/chat")
	public String myChat(Model model) throws Exception {
		log.info("Start chat");
		File file = new File(chat.resultPath());
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

}
