package michael.slf4j.investment.etl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.constant.Constants;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.TradeUtil;

@Component("futureLoader")
public class FutureLoader {
	private static final Logger log = Logger.getLogger(FutureLoader.class);

	@Autowired
	private TimeseriesRepository timeseriesRepository;

	private Map<String, Timeseries> previousMap = new ConcurrentHashMap<>();
	
	public boolean loadMultiSecurities(IParser parser, String content, FreqEnum freq) {
		if(!TradeUtil.isTradingTime()) {
			return false;
		}
		List<Timeseries> series = parser.parse(content, freq);
		return loadMultiSecurities(series, freq);
	}
	
	public boolean loadMultiSecurities(List<Timeseries> series, FreqEnum freq) {
		if(!TradeUtil.isTradingTime()) {
			return false;
		}
		List<Timeseries> availableSeries = series;
		if(freq == FreqEnum._TICK) {
			availableSeries = series.stream().filter(m -> !(previousMap.get(m.getSecurity()) != null && m.equals(previousMap.get(m.getSecurity()))))
				.collect(Collectors.toList());
		}
		StringBuffer sb = new StringBuffer();
		availableSeries.forEach(m -> {
			previousMap.put(m.getSecurity(), m);
			sb.append(m.getSecurity());
			sb.append(" ");
		});
		timeseriesRepository.saveAll(availableSeries);
		log.info("load[" + sb.toString().trim() + "] for [" + freq + "] successful.");
		return true;
	}
	
	public boolean loadSecurity(Security security, FreqEnum freq, List<Timeseries> series) {
		if(!TradeUtil.isTradingTime()) {
			return false;
		}
		List<Timeseries> storedData = timeseriesRepository.findBySecurityFreqLimit(security.getName(), freq.getValue(), 300);
		for (Timeseries ts : series) {
			boolean find = false;
			for (Timeseries tsInDB : storedData) {
				if(tsInDB.getTradeTs().equals(ts.getTradeTs())) {
					find = true;
					tsInDB.setClose(ts.getClose());
					tsInDB.setHigh(ts.getHigh());
					tsInDB.setLow(ts.getLow());
					tsInDB.setOpenInterest(ts.getOpenInterest());
					tsInDB.setVolume(ts.getVolume());
					break;
				}
			}
			if(!find) {
				storedData.add(ts);
			}
		}
		log.info("load[" + security.getName() + "] for [" + freq + "] successful.");
		timeseriesRepository.saveAll(storedData);
		return true;
	}
	
	public List<Timeseries> getSecuritySeries(String security, String freq, int limit){
		return timeseriesRepository.findBySecurityFreqLimit(security, freq, limit);
	}
	
	public void fillBack1D() {
		for (String variety : Constants.VARIETY_LIST) {
			List<String> tradeDateList = timeseriesRepository.findMaxTradeDate(variety);
			fillBack1D(variety, tradeDateList);
		}
	}
	
	public void fillBack1D(String variety, List<String> tradeDateList) {
		log.info("Start to clean [" + variety + "] data.");
		tradeDateList.stream().forEach(tradeDate -> {
			List<String> securites = timeseriesRepository.findSecurities(variety, tradeDate);
			securites.stream().forEach(security -> {
				List<Timeseries> eodList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, "1D");
				if(eodList.isEmpty()) {
					List<Timeseries> tickList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, "TICK");
					Timeseries latest = null;
					if(!tickList.isEmpty()) {
						latest = tickList.get(tickList.size() - 1).copy();
					} else {
						List<Timeseries> miList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, "1MI");
						latest = miList.get(miList.size() - 1).copy();
					}
					latest.setFreq("1D");
					timeseriesRepository.save(latest);
					log.info("Update for security[" + security + "," + tradeDate + "]");
				}
			});
		});
		log.info("complete to update[" + variety + "].");
	}

}
