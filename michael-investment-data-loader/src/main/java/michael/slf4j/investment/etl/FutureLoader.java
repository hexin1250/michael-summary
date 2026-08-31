package michael.slf4j.investment.etl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.repo.TimeseriesRepository;

@Component("futureLoader")
public class FutureLoader {
	private static final Logger log = Logger.getLogger(FutureLoader.class);

	@Autowired
	private TimeseriesRepository timeseriesRepository;

	private Map<String, Timeseries> previousMap = new ConcurrentHashMap<>();
	
	public boolean loadMultiSecurities(IParser parser, String content, FreqEnum freq) {
		List<Timeseries> series = parser.parse(content, freq);
		return loadMultiSecurities(series, freq);
	}
	
	public boolean loadMultiSecurities(List<Timeseries> series, FreqEnum freq) {
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
		return loadSecurity(security, freq, series, 100);
	}
	
	public boolean loadSecurity(Security security, FreqEnum freq, List<Timeseries> series, int limit) {
		List<Timeseries> storedData = timeseriesRepository.findBySecurityFreqLimit(security.getName(), freq.getValue(), limit);
		for (Timeseries ts : series) {
			boolean find = false;
			for (Timeseries tsInDB : storedData) {
				if(tsInDB.getTradeTs().equals(ts.getTradeTs())) {
					find = true;
					tsInDB.setClose(ts.getClose());
					tsInDB.setHigh(ts.getHigh());
					tsInDB.setLow(ts.getLow());
					tsInDB.setOpen(ts.getOpen());
					if(ts.getOpenInterest() != null && ts.getOpenInterest().intValue() > 0) {
						tsInDB.setOpenInterest(ts.getOpenInterest());
					}
					tsInDB.setVolume(ts.getVolume());
					break;
				}
			}
			if(!find) {
				ts.setOpenInterest(new BigDecimal(1));
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

}
