package michael.slf4j.investment.quant.backtest;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.model.TimeseriesModel;
import michael.slf4j.investment.model.TradeDeal;
import michael.slf4j.investment.quant.BacktestRequest;
import michael.slf4j.investment.repo.TimeseriesRepository;

@Controller
public class FutureStrategy {
	private static final Logger log = Logger.getLogger(FutureStrategy.class);
	
	@Autowired
	public TimeseriesRepository timeseriesRepository;
	
	private AtomicInteger increaseAtomic = new AtomicInteger();
	private Map<Integer, Map<String, Double>> runMap = new ConcurrentHashMap<>();
	private Map<Integer, TradeDeal> lastTradeMap = new ConcurrentHashMap<>();
	
	public void mockup(BacktestRequest request, int dataScope, double k) {
		int nextInt = increaseAtomic.getAndIncrement();
		runMap.put(nextInt, new ConcurrentHashMap<>());
		List<String> tradeDateList = timeseriesRepository.findAllTradeDateByVariety(request.getVariety());
		String accountFuture = null;
		/**
		 * buy for 1, sell for -1
		 */
		int direction = 0;
		String startDate = request.getStartDate();
		String endDate = request.getEndDate();
		for (String tradeDate : tradeDateList) {
			if(tradeDate.compareTo(startDate) < 0 || tradeDate.compareTo(endDate) > 0) {
				continue;
			}
			String primarySecurity = timeseriesRepository.findMainFutureByVarietyDate(request.getVariety(), tradeDate);
			try {
				String security = primarySecurity;
				Double range = getRangeValue(nextInt, security, tradeDate, dataScope);
				if(range != null) {
					List<TimeseriesModel> modelList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, request.getFreq());
					TimeseriesModel first = modelList.get(0);
					if(accountFuture == null) {
						accountFuture = security;
					} else if(accountFuture != null && !accountFuture.equals(security)) {
						changeDominate(direction, accountFuture, first);
						accountFuture = security;
					}
					double buyLine = first.getOpen().doubleValue() + range * k;
					double sellLine = first.getOpen().doubleValue() - range * k;
					log.info(tradeDate + ":" + "Buy Line->" + buyLine + " vs Sell Line->" + sellLine);
					for (TimeseriesModel model : modelList) {
						double closePrice = model.getClose().doubleValue();
						if(closePrice > buyLine && direction <= 0) {
							direction = 1;
							log.info(tradeDate + "->Buy:[" + security + "], Price:[" + closePrice + "] at " + model.getTradeTs() + ".");
						} else if(closePrice < sellLine && direction >= 0) {
							direction = -1;
							log.info(tradeDate + "->Sell[" + security + "], Price:[" + closePrice + "] at " + model.getTradeTs() + ".");
						}
					}
				}
			} catch(RuntimeException e) {
				log.error("Date:" + tradeDate);
				throw e;
			}
		}
		log.info("Backtest Done.");
		runMap.remove(nextInt);
	}
	
	private void changeDominate(int direction, String accountFuture, TimeseriesModel first) {
		String tradeDate = first.getTradeDate();
		Timestamp ts = first.getTradeTs();
		List<TimeseriesModel> modelList = timeseriesRepository.findByTradeDateWithPeriod(accountFuture, tradeDate, "1MI");
		TimeseriesModel latest = modelList.get(modelList.size() - 1);
		if(direction < 0) {
			log.info(tradeDate + "[change dominate]->Buy:[" + accountFuture + "], Price:[" + latest.getClose() + "] at " + ts + ".");
			log.info(tradeDate + "[change dominate]->Sell:[" + first.getSecurity() + "], Price:[" + first.getClose() + "] at " + ts + ".");
		} else if(direction > 0) {
			log.info(tradeDate + "[change dominate]->Sell:[" + accountFuture + "], Price:[" + latest.getClose() + "] at " + ts + ".");
			log.info(tradeDate + "[change dominate]->Buy:[" + first.getSecurity() + "], Price:[" + first.getClose() + "] at " + ts + ".");
		}
		
	}

	public Double getRangeValue(int runId, String security, String tradeDate, int dataScope) {
		Map<String, Double> rangeMap = runMap.get(runId);
		Double ret = rangeMap.get(tradeDate);
		if(ret != null) {
			return ret;
		}
		
		String freq = "1D";
		List<String> allTradeDateList = timeseriesRepository.findAllTradeDate();
		List<String> subList = allTradeDateList.stream().filter(d -> d.compareTo(tradeDate) <= 0).sorted().collect(Collectors.toList());
		if(subList.size() < dataScope + 1) {
			return null;
		}
		int newDateScope = dataScope - 1;
		List<String> targetList = IntStream.range(0, newDateScope).mapToObj(i -> subList.get(subList.size() - i - 3)).sorted().collect(Collectors.toList());
		List<TimeseriesModel> modelList = timeseriesRepository.findByTradeDateWithPeriod(security, targetList.get(0), targetList.get(newDateScope - 1), freq);
		double hh = modelList.stream().mapToDouble(m -> m.getHigh().doubleValue()).max().getAsDouble();
		double ll = modelList.stream().mapToDouble(m -> m.getLow().doubleValue()).min().getAsDouble();
		double lc = modelList.stream().mapToDouble(m -> m.getClose().doubleValue()).min().getAsDouble();
		double hc = modelList.stream().mapToDouble(m -> m.getClose().doubleValue()).max().getAsDouble();
		double rangeOfValue = Math.max((hh - lc), (hc - ll));
		rangeMap.put(tradeDate, rangeOfValue);
		return rangeOfValue;
	}

}
