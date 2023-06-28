package michael.slf4j.investment.quant.live;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.exception.NotSubscribeException;
import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Context;
import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.DirectionEnum;
import michael.slf4j.investment.model.RealRun;
import michael.slf4j.investment.model.RealRunTxn;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Status;
import michael.slf4j.investment.model.StrategyType;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.strategy.IStrategy;
import michael.slf4j.investment.repo.RealRunRepository;
import michael.slf4j.investment.repo.RealRunTxnRepository;
import michael.slf4j.investment.util.HolidayUtil;
import michael.slf4j.investment.util.TradeUtil;

@Component("liveProcessor")
public class LiveProcessor {
	private static final Logger log = Logger.getLogger(LiveProcessor.class);
	
	@Autowired
	private RealRunRepository realRunRepo;
	
	@Autowired
	private RealRunTxnRepository realRunTxnRepo;
	
	private Map<String, StrategyType> centerMap = new ConcurrentHashMap<>();
	private Map<String, IStrategy> strategyMap = new ConcurrentHashMap<>();
	private Map<String, Account> accMap = new ConcurrentHashMap<>();
	private Map<String, Context> contextMap = new ConcurrentHashMap<>();
	private Map<String, Bar> barMap = new ConcurrentHashMap<>();
	
	public void init() {
		log.info("Start to initialize live trade processor");
		List<RealRun> runList = realRunRepo.findRunningJobs();
		runList.stream().forEach(rr -> {
			initStrategy(rr);
		});
		log.info("Done to initialize live trade processor");
	}

	public void initStrategy(RealRun rr) {
		String strategyName = rr.getName();
		long runId = rr.getId();
		/**
		 * Init Account
		 */
		Account acc = new Account(runId, rr.getInitCash().doubleValue(), realRunTxnRepo);
		/**
		 * Init Context
		 */
		Context context = new Context(rr.getId(), acc);
		
		List<RealRunTxn> rrtList = realRunTxnRepo.findByRealRunId(runId);
		rrtList.stream().forEach(rrt -> {
			Variety variety = Variety.of(rrt.getVariety());
			Security security = new Security(rrt.getSecurity(), variety);
			DirectionEnum direction = DirectionEnum.of(rrt.getDirection());
			acc.deal(security, direction, rrt.getDealPrice().doubleValue(), rrt.getDealCount(), rrt.getTradeDate(), rrt.getTradeTs());
		});
		acc.setPersistent(true);
		/**
		 * Init Strategy
		 */
		String className = rr.getClassName();
		try {
			@SuppressWarnings("unchecked")
			Constructor<IStrategy> [] constructors = (Constructor<IStrategy>[]) Class.forName(className).getConstructors();
			if(constructors.length > 1) {
				throw new IllegalArgumentException("Only 1 Strategy Constructor and default Constructor is allowed!");
			}
			Constructor<IStrategy> constructor = constructors[0];
			IStrategy strategy = constructor.newInstance();
			strategy.initContext(context);
			strategy.init();
			context.init();
			centerMap.put(strategyName, StrategyType.of(rr.getType()));
			strategyMap.put(strategyName, strategy);
			contextMap.put(strategyName, context);
			accMap.put(strategyName, acc);
		} catch (SecurityException | ClassNotFoundException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			log.error("This stragety[" + strategyName + ":" + className + "] will not be created", e);
			return;
		}
		log.info("Initialize strategy:" + strategyName);
		beforeTrading();
	}
	
	public void beforeTrading() {
		if(centerMap.isEmpty()) {
			init();
		}
		log.info("Start to initialize before trading");
		LocalDateTime ldt = LocalDateTime.now();
		LocalDate tradeDate = HolidayUtil.$.getCurrentTradeDate(TradeUtil.getCurrentTradeDate(ldt));
		centerMap.entrySet().parallelStream().forEach(entry -> {
			String strategyName = entry.getKey();
			IStrategy strategy = strategyMap.get(strategyName);
			Context context = contextMap.get(strategyName);
			context.setCurrentTradeDate(tradeDate);
			Status.updateTime(context.runId, ldt, tradeDate);
			strategy.subscriber(context, tradeDate);
			strategy.before();
			List<Security> securityList = strategy.subscriberList(tradeDate);
			Bar bar = new Bar();
			bar.subscribe(securityList);
			barMap.put(strategyName, bar);
		});
		log.info("Done to initialize before trading");
	}
	
	public void handle(StrategyType type, Map<Security, Contract> contractMap) {
		log.info("Start to handle trade");
		LocalDateTime ldt = LocalDateTime.now();
		centerMap.entrySet().stream().filter(entry -> entry.getValue() == type).forEach(entry -> {
			String strategyName = entry.getKey();
			updateBar(strategyName, contractMap);
			Context context = contextMap.get(strategyName);
			Status.updateStatus(context.runId, contractMap, ldt, context.getCurrentTradeDate());
			handle(strategyName);
		});
		log.info("Done to handle trade");
	}
	
	public void handle(String strategyName) {
		log.info("handling:" + strategyName);
		IStrategy strategy = strategyMap.get(strategyName);
		Account acc = accMap.get(strategyName);
		Bar bar = barMap.get(strategyName);
		strategy.handle(acc, bar);
		log.info("Done to handle:" + strategyName);
	}
	
	public void afterTrading() {
		log.info("Start to initialize after trading");
		centerMap.keySet().parallelStream().forEach(strategyName -> {
			Context context = contextMap.get(strategyName);
			context.historical.update(context.getCurrentTradeDate());
			Map<Security, Contract> status = context.historical.getEodStatus();
			Account acc = accMap.get(strategyName);
			log.info(context.getCurrentTradeDate() + ":\t" + strategyName + ":" + acc.total(status));
			IStrategy strategy = strategyMap.get(strategyName);
			strategy.after();
		});
		log.info("Done to initialize after trading");
	}

	public Map<String, StrategyType> getAllStrategy() {
		return centerMap;
	}
	
	public Map<String, StrategyType> getAllStrategy(StrategyType type) {
		return centerMap.entrySet().stream().filter(entry -> entry.getValue() == type).collect(Collectors.toConcurrentMap(entry -> entry.getKey(), entry -> entry.getValue()));
	}
	
	public void updateBar(String strategyName, Map<Security, Contract> contractMap) {
		Bar bar = barMap.get(strategyName);
		contractMap.entrySet().stream().forEach(entry -> {
			try {
				bar.update(entry.getKey(), entry.getValue());
			} catch (NotSubscribeException e) {
				/**
				 * this can be ignored
				 */
			}
		});
	}

}
