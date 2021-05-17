package michael.slf4j.investment.quant.strategy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.model.TimeseriesModel;
import michael.slf4j.investment.repo.TimeseriesRepository;

@Controller
public class FutureStrategy {
	private static final Logger log = Logger.getLogger(FutureStrategy.class);
	
	@Autowired
	public TimeseriesRepository timeseriesRepository;
	
	private Map<String, Double> rangeMap = new ConcurrentHashMap<>();
	
	public void mockup(String variety, String freq, int dataScope, double k) {
		List<String> tradeDateList = timeseriesRepository.findAllTradeDateByVariety(variety);
		int direction = 0;
		for (String tradeDate : tradeDateList) {
			TimeseriesModel primaryContract = timeseriesRepository.findMainFutureByVarietyDate(variety, tradeDate);
			try {
				String security = primaryContract.getSecurity();
				Double range = getRangeValue(security, tradeDate, dataScope);
				if(range != null) {
					List<TimeseriesModel> modelList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, freq);
					TimeseriesModel first = modelList.get(0);
					double buyLine = first.getOpen().doubleValue() + range * k;
					double sellLine = first.getOpen().doubleValue() - range * k;
//					log.info("Date:" + tradeDate + ", open:" + first.getOpen().doubleValue() + ",future[" + security + "], buy line:" + buyLine + ", sell line:" + sellLine);
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
	}
	
	public Double getRangeValue(String security, String tradeDate, int dataScope) {
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

	public void clear() {
		rangeMap.clear();
	}

}
