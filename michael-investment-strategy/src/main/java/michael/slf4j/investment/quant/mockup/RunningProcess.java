package michael.slf4j.investment.quant.mockup;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.model.Account;
import michael.slf4j.investment.model.Bar;
import michael.slf4j.investment.model.Context;
import michael.slf4j.investment.model.Contract;
import michael.slf4j.investment.model.Future;
import michael.slf4j.investment.model.Status;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.TradeDateEnum;
import michael.slf4j.investment.model.TradePairEnum;
import michael.slf4j.investment.quant.strategy.IStrategy;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.HolidayUtil;
import michael.slf4j.investment.util.TradeUtil;

@Controller
public class RunningProcess {
	private static final Logger log = Logger.getLogger(RunningProcess.class);
	
	private AtomicInteger atom = new AtomicInteger();
	private Map<Integer, Bar> barMap = new ConcurrentHashMap<>();
	
	@Autowired
	private TimeseriesRepository repo;
	
	public void backtest(Account acc, IStrategy strategy, LocalDate startDate, LocalDate endDate) {
		int runId = atom.getAndIncrement();
		Bar bar = new Bar();
		barMap.put(runId, bar);
		LocalDate current = startDate;
		Context context = new Context(runId, acc);
		strategy.init(context);
		context.init(strategy.getParams());
		TradePairEnum[] nightPair = TradeDateEnum.night.getTradingHours();
		TradePairEnum[] dayPair = TradeDateEnum.day.getTradingHours();
		TradePairEnum[] pairs = new TradePairEnum[nightPair.length + dayPair.length];
		for (int i = 0; i < nightPair.length; i++) {
			pairs[i] = nightPair[i];
		}
		for (int i = nightPair.length; i < pairs.length; i++) {
			pairs[i] = dayPair[i - nightPair.length];
		}
		while(!current.isAfter(endDate)) {
			if(TradeUtil.isWeekend(current) || HolidayUtil.$.isHoliday(current)) {
				current = current.plusDays(1);
				continue;
			}
			LocalDate previousTradeDate = TradeUtil.previousTradeDate(current);
			while(HolidayUtil.$.isHoliday(previousTradeDate)) {
				previousTradeDate = TradeUtil.previousTradeDate(previousTradeDate);
			}
			strategy.subscriber(context, current);
			strategy.before(context, current);
			List<String> securityList = strategy.subscriberList(current).stream().map(security -> security.getName()).collect(Collectors.toList());
			bar.subscribe(securityList);
			Map<LocalTime, Map<String, Contract>> map = getTradingMap(securityList, current);
			for (TradePairEnum tradePairEnum : pairs) {
				LocalTime currentLt = tradePairEnum.getStart();
				while(!currentLt.isAfter(tradePairEnum.getEnd())) {
					if((currentLt.getHour() == 9 && currentLt.getMinute() == 0) || (currentLt.getHour() == 21 && currentLt.getMinute() == 0)) {
						currentLt = currentLt.plusMinutes(1);
						continue;
					}
					Map<String, Contract> tradingMap = map.remove(currentLt);
					if(tradingMap == null) {
						currentLt = currentLt.plusMinutes(1);
						continue;
					}
					tradingMap.entrySet().forEach(entry -> bar.update(entry.getKey(), entry.getValue()));
					LocalDateTime ldt = null;
					if(currentLt.getHour() >= 21) {
						ldt = LocalDateTime.of(previousTradeDate, currentLt);
					} else {
						ldt = LocalDateTime.of(current, currentLt);
					}
					Status.updateStatus(runId, bar.map, ldt);
					strategy.handle(acc, bar);
					currentLt = currentLt.plusMinutes(1);
				}
			}
			strategy.after(context, current);
			context.historical.update(current);
			Map<String, Contract> status = context.historical.getEodStatus();
			log.info(current + ":" + acc.total(status));
			
			current = current.plusDays(1);
		}
		Status.unregister(runId);
	}
	
	private Map<LocalTime, Map<String, Contract>> getTradingMap(List<String> securityList, LocalDate tradeDate){
		List<Timeseries> models = repo.findSecuritiesBySecurities(securityList, tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "1MI");
		Map<LocalTime, Map<String, Contract>> map = new HashMap<>();
		models.stream().forEach(model -> {
			Timestamp ts = model.getTradeTs();
			LocalDateTime ldt = LocalDateTime.ofInstant(new java.util.Date(ts.getTime()).toInstant(), ZoneId.systemDefault());
			LocalTime lt = LocalTime.of(ldt.getHour(), ldt.getMinute(), 0);
			String security = model.getSecurity();
			Contract contract = new Future(model);
			Map<String, Contract> timingMap = map.get(lt);
			if(timingMap == null) {
				timingMap = new HashMap<>();
				map.put(lt, timingMap);
			}
			timingMap.put(security, contract);
		});
		return map;
	}

}