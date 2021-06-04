package michael.slf4j.investment.etl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.constant.Constants;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.taskmanager.FutureTask;
import michael.slf4j.investment.util.TradeUtil;

@Controller
public class FutureLoader {
	private static final Logger log = Logger.getLogger(FutureTask.class);

	@Autowired
	private TimeseriesRepository timeseriesRepository;

	private Map<String, Timeseries> previousMap = new ConcurrentHashMap<>();
	private Map<String, String> primarySecurityMap = new ConcurrentHashMap<>();
	
	public void init() {
		log.info("Initialize primary security.");
		primarySecurityMap.clear();
		for (String variety : Constants.VARIETY_LIST) {
			String currentTradeDate = TradeUtil.getDateStr(TradeUtil.getTradeDate());
			List<String> latestTradeDateList = timeseriesRepository.findMaxTradeDate(variety);
			String lastTradeDate = null;
			for (String tradeDate : latestTradeDateList) {
				if(!tradeDate.equals(currentTradeDate)) {
					lastTradeDate = tradeDate;
					break;
				}
			}
			if(lastTradeDate != null) {
				String primarySecurity = timeseriesRepository.findPrimarySecurity(variety, lastTradeDate);
				primarySecurityMap.put(variety, primarySecurity);
			}
		}
	}
	
	public boolean load(String variety, String security, String content, FreqEnum freq) {
		String primarySecurity = primarySecurityMap.get(variety);
		Timeseries m = generateModel(security, content, freq);
		m.setIsMainFuture(security.equals(primarySecurity) ? "T" : "F");
		switch(freq) {
		case _TICK:
			if(previousMap.get(security) != null && (!TradeUtil.isTradingTime() || m.equals(previousMap.get(security)))) {
				return false;
			}
			previousMap.put(security, m);
			default:
				break;
		}
		timeseriesRepository.save(m);
		log.info("load[" + security + "] for [" + freq + "] successful.");
		return true;
	}
	
	private Timeseries generateModel(String security, String content, FreqEnum freq) {
		String[] parts = content.split(",");
		Timeseries m = new Timeseries();
		m.setSecurity(security);
		m.setVariety(security.replaceAll("[\\d]+", ""));
		m.setSecurityName(parts[0]);
		m.setOpen(new BigDecimal(parts[2]));
		m.setHigh(new BigDecimal(parts[3]));
		m.setLow(new BigDecimal(parts[4]));
		m.setClose(new BigDecimal(parts[8]));
		m.setOpenInterest(new BigDecimal(parts[13]));
		m.setVolume(new BigDecimal(parts[14]));
		BigDecimal buy1 = new BigDecimal(parts[6]);
		BigDecimal sell1 = new BigDecimal(parts[7]);
		if (buy1.compareTo(new BigDecimal(0)) == 0) {
			m.setDownLimit(new BigDecimal(parts[8]));
		}
		if (sell1.compareTo(new BigDecimal(0)) == 0) {
			m.setUpLimit(new BigDecimal(parts[8]));
		}
		m.setFreq(freq.getValue());
		
		m.setTradeDate(TradeUtil.getDateStr(TradeUtil.getTradeDate()));
		m.setTradeTs(new Timestamp(System.currentTimeMillis()));
		return m;
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
					if(!tickList.isEmpty()) {
						Timeseries latest = tickList.get(tickList.size() - 1).copy();
						latest.setFreq("1D");
						timeseriesRepository.save(latest);
						log.info("Update for security[" + security + "," + tradeDate + "]");
					} else {
						List<Timeseries> miList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, "1MI");
						Timeseries latest = miList.get(miList.size() - 1).copy();
						latest.setFreq("1D");
						timeseriesRepository.save(latest);
						log.info("Update for security[" + security + "," + tradeDate + "]");
					}
				}
			});
		});
		log.info("complete to update[" + variety + "].");
	}

}
